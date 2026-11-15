import { useEffect, useRef, useState } from 'react';
import { PageHeader } from '../components/layout/PageHeader';
import { Sidebar } from '../components/layout/Sidebar';
import { CommentsPanel } from '../features/comments/CommentsPanel';
import { ExtractionForm } from '../features/extraction/ExtractionForm';
import { ExtractionHero } from '../features/extraction/ExtractionHero';
import { useFacebookExtraction } from '../hooks/useFacebookExtraction';
import { useExtractionJobFeed } from '../hooks/useExtractionJobFeed';
import { canUseTool, isAdmin, isPending } from '../lib/access';
import { workspaceApi } from '../lib/apiClient';
import { LeadsPage } from './LeadsPage';
import { HistoryPage } from './HistoryPage';
import { ToolsPage } from './ToolsPage';
import { AdminPage } from './AdminPage';
import { LeadSignalsPage } from './LeadSignalsPage';
import { ContentMonitorPage } from './ContentMonitorPage';
import { DataCenterPage } from './DataCenterPage';
import { ExtractionJobsPage } from './ExtractionJobsPage';
import { useI18n } from '../i18n/I18nContext';

const routes = {
  'Kho công cụ': '#/tools',
  'Extract comment': '#/tools/extract',
  'Lead scoring': '#/tools/extract/signals',
  'Content monitor': '#/tools/content',
  'Khách hàng tiềm năng': '#/data/leads',
  'Dữ liệu workspace': '#/data',
  'Lịch sử': '#/history',
  'Tác vụ': '#/jobs',
  'Quản trị': '#/admin'
};

function pageFromRoute(hash) {
  if (hash === '#/tools/extract/signals') return 'Lead scoring';
  if (hash === '#/tools/content/following') return 'Content monitor';
  if (/^#\/history\/\d+$/.test(hash)) return 'Lịch sử';
  if (/^#\/jobs\/[\w-]+$/.test(hash)) return 'Tác vụ';
  return Object.entries(routes).find(([, route]) => route === hash)?.[0] ?? 'Kho công cụ';
}

function historyIdFromRoute(hash) {
  const match = /^#\/history\/(\d+)$/.exec(hash);
  return match ? Number(match[1]) : null;
}

function jobIdFromRoute(hash) {
  return /^#\/jobs\/([\w-]+)$/.exec(hash)?.[1] ?? null;
}

export function DashboardPage({ user, workspace, users = [], requests = [], onRequestFeature, onDecideUser, onDecideRequest, onLogout, onManageWorkspaces, language, onLanguageChange, theme, onThemeChange }) {
  const [activePage, setActivePage] = useState(() => pageFromRoute(window.location.hash));
  const [selectedHistoryId, setSelectedHistoryId] = useState(() => historyIdFromRoute(window.location.hash));
  const [selectedJobId, setSelectedJobId] = useState(() => jobIdFromRoute(window.location.hash));
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const workspaceId = workspace.id;
  const [savedLeads, setSavedLeads] = useState([]);
  const [suggestedComments, setSuggestedComments] = useState([]);
  const [history, setHistory] = useState([]);
  const [monitorResults, setMonitorResults] = useState([]);
  const [monitorHistory, setMonitorHistory] = useState([]);
  const [extractView, setExtractView] = useState('extract');
  const [monitorView, setMonitorView] = useState('search');
  const { t } = useI18n();
  const mainRef = useRef(null);
  const extraction = useFacebookExtraction(user.id, workspaceId, user.token);
  const jobFeed = useExtractionJobFeed(workspaceId, user.token, canUseTool(user, 'Extract comment'));
  const comments = extraction.result?.comments ?? [];
  const savedIds = new Set(savedLeads.map((lead) => lead.id));
  const unsavedSuggestionCount = suggestedComments.filter((item) => !savedIds.has(item.comment.id)).length;
  const admin = isAdmin(user);
  const pendingCount = users.filter(isPending).length + requests.filter(isPending).length;
  const canUse = (tool) => canUseTool(user, tool);
  const requested = (tool) => requests.some((request) => request.userId === user.id && (request.tool ?? request.feature) === tool && isPending(request));

  useEffect(() => {
    if (!sidebarOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const close = (event) => { if (event.key === 'Escape') setSidebarOpen(false); };
    window.addEventListener('keydown', close);
    return () => { document.body.style.overflow = previousOverflow; window.removeEventListener('keydown', close); };
  }, [sidebarOpen]);

  useEffect(() => {
    const syncRoute = () => { setActivePage(pageFromRoute(window.location.hash)); setSelectedHistoryId(historyIdFromRoute(window.location.hash)); setSelectedJobId(jobIdFromRoute(window.location.hash)); };
    window.addEventListener('hashchange', syncRoute);
    if (!window.location.hash) window.history.replaceState(null, '', routes['Kho công cụ']);
    return () => window.removeEventListener('hashchange', syncRoute);
  }, []);

  useEffect(() => {
    if (activePage === 'Lead scoring') setExtractView('signals');
    if (activePage === 'Extract comment') setExtractView('extract');
    if (activePage === 'Content monitor') setMonitorView(window.location.hash === '#/tools/content/following' ? 'following' : 'search');
  }, [activePage]);

  useEffect(() => {
    Promise.all([workspaceApi.leads(workspaceId, user.token), workspaceApi.history(workspaceId, user.token), workspaceApi.suggestedComments(workspaceId, user.token), workspaceApi.monitor(workspaceId, user.token)])
      .then(([leadData, historyData, suggestions, monitor]) => {
        setSavedLeads(leadData.map((lead) => ({ ...lead, id: lead.externalCommentId, recordId: lead.id, parentId: lead.parentCommentId, createdAt: lead.commentCreatedAt })));
        setHistory(historyData.map((item) => ({ ...item, url: item.postUrl, count: item.commentCount })));
        setSuggestedComments(suggestions);
        setMonitorResults(monitor.results);
        setMonitorHistory(monitor.history);
      }).catch(() => onLogout());
  }, [workspaceId, user.token]);

  useEffect(() => {
    if (extraction.job?.status !== 'COMPLETED') return;
    Promise.all([workspaceApi.history(workspaceId, user.token), workspaceApi.suggestedComments(workspaceId, user.token)])
      .then(([items, suggestions]) => { setHistory(items.map((item) => ({ ...item, url: item.postUrl, count: item.commentCount }))); setSuggestedComments(suggestions); })
      .catch(() => {});
  }, [extraction.job?.id, extraction.job?.status, workspaceId, user.token]);

  useEffect(() => {
    if (!jobFeed.finishedEvent?.jobs.some((job) => job.status === 'COMPLETED')) return;
    Promise.all([workspaceApi.history(workspaceId, user.token), workspaceApi.suggestedComments(workspaceId, user.token)])
      .then(([items, suggestions]) => { setHistory(items.map((item) => ({ ...item, url: item.postUrl, count: item.commentCount }))); setSuggestedComments(suggestions); })
      .catch(() => {});
  }, [jobFeed.finishedEvent?.at, workspaceId, user.token]);

  function navigate(page) {
    if (page === 'Quản trị' && !admin) return;
    if (['Extract comment', 'Lead scoring', 'Content monitor'].includes(page) && !canUse(page)) page = 'Kho công cụ';
    const targetRoute = routes[page] ?? routes['Kho công cụ'];
    if (window.location.hash !== targetRoute) window.location.hash = targetRoute;
    setActivePage(page);
    if (page === 'Lịch sử') setSelectedHistoryId(null);
    if (page === 'Tác vụ') setSelectedJobId(null);
    if (page === 'Lead scoring') setExtractView('signals');
    if (page === 'Extract comment') setExtractView('extract');
    setSidebarOpen(false);
    mainRef.current?.focus();
    window.scrollTo({ top: 0, behavior: 'auto' });
  }

  function openHistory(id) {
    window.location.hash = `#/history/${id}`;
    setActivePage('Lịch sử');
    setSelectedHistoryId(id);
    window.scrollTo({ top: 0, behavior: 'auto' });
  }

  function openJob(id) {
    window.location.hash = `#/jobs/${id}`;
    setActivePage('Tác vụ');
    setSelectedJobId(id);
    window.scrollTo({ top: 0, behavior: 'auto' });
  }

  async function runExtraction() {
    if (!canUse('Extract comment')) return;
    const submitted = await extraction.extractComments();
    if (submitted) jobFeed.refresh();
  }

  async function toggleLead(comment) {
    const existing = savedLeads.find((lead) => lead.id === comment.id);
    if (existing) {
      await workspaceApi.removeLead(workspaceId, existing.recordId, user.token);
      setSavedLeads((current) => current.filter((lead) => lead.id !== comment.id));
      return;
    }
    const lead = await workspaceApi.saveLead(workspaceId, { externalCommentId: comment.id, author: comment.author, authorUrl: comment.authorUrl, postUrl: comment.postUrl ?? extraction.job?.postUrl, message: comment.message, parentCommentId: comment.parentId, commentCreatedAt: comment.createdAt }, user.token);
    setSavedLeads((current) => [{ ...lead, id: lead.externalCommentId, recordId: lead.id, parentId: lead.parentCommentId, createdAt: lead.commentCreatedAt }, ...current]);
  }

  return <div className="shell workspace-shell">
    <a className="skip-link" href="#workspace-main">{t('common.skipContent')}</a>
    {sidebarOpen && <button className="sidebar-backdrop" aria-label={t('common.closeMenu')} onClick={() => setSidebarOpen(false)} />}
    <Sidebar activePage={activePage} onNavigate={navigate} pendingCount={pendingCount} isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} onLogout={onLogout} user={user} workspace={workspace} onManageWorkspaces={onManageWorkspaces} language={language} onLanguageChange={onLanguageChange} theme={theme} onThemeChange={onThemeChange} />
    <main id="workspace-main" ref={mainRef} tabIndex={-1} className="content">
      <PageHeader activePage={activePage} workspace={workspace} onMenu={() => setSidebarOpen(true)} menuOpen={sidebarOpen} user={user} />
      {jobFeed.activeCount > 0 && <div className="extraction-activity running">
        <span className="activity-pulse" />
        {t('jobs.activeNotice', { count: jobFeed.activeCount })}
        <button type="button" onClick={() => navigate('Tác vụ')}>{t('jobs.view')}</button>
      </div>}
      {jobFeed.finishedEvent && <div className="extraction-activity completed" role="status">
        ✓ {t('jobs.finishedNotice', { count: jobFeed.finishedEvent.jobs.length })}
        <button type="button" onClick={() => { jobFeed.dismissFinished(); navigate('Tác vụ'); }}>{t('jobs.view')}</button>
        <button type="button" aria-label={t('common.close')} onClick={jobFeed.dismissFinished}>×</button>
      </div>}
      <div className="workspace-page" key={activePage}>
        {activePage === 'Kho công cụ' && <ToolsPage user={user} onOpenTool={navigate} onRequestTool={onRequestFeature} canUse={canUse} requested={requested} resultCount={comments.length} leadCount={savedLeads.length} historyCount={history.length} />}
        {(activePage === 'Extract comment' || activePage === 'Lead scoring') && canUse('Extract comment') && <>
          <ToolTabs active={extractView} onChange={(view) => navigate(view === 'signals' ? 'Lead scoring' : 'Extract comment')} tabs={[['extract', t('extract.tab')], ['signals', t('extract.signalsTab')]]} />
          {extractView === 'extract' && <>
          <ExtractionHero />
          <ExtractionForm form={extraction.form} isLoading={extraction.isExtracting} notice={extraction.notice} onChange={extraction.updateField} onSubmit={runExtraction} />
          <CommentsPanel comments={comments} savedIds={savedIds} onToggleSave={toggleLead} />
          </>}
          {extractView === 'signals' && <LeadSignalsPage comments={comments} savedIds={savedIds} onToggleSave={toggleLead} onExtract={() => setExtractView('extract')} canExtract={canUse('Extract comment')} />}
        </>}
        {activePage === 'Content monitor' && canUse('Content monitor') && <><ToolTabs active={monitorView} onChange={(view) => { window.location.hash = view === 'following' ? '#/tools/content/following' : '#/tools/content'; setMonitorView(view); }} tabs={[['search', t('monitor.searchTab')], ['following', t('monitor.followingTab')]]} /><ContentMonitorPage results={monitorResults} history={monitorHistory} onResultsChange={setMonitorResults} onHistoryChange={setMonitorHistory} workspaceId={workspaceId} token={user.token} view={monitorView} onUseForExtraction={(url) => { extraction.updateField('postUrl', url); setExtractView('extract'); navigate('Extract comment'); }} /></>}
        {activePage === 'Dữ liệu workspace' && <DataCenterPage
          workspace={workspace}
          leads={savedLeads}
          suggestionCount={unsavedSuggestionCount}
          history={history}
          jobs={canUse('Extract comment') ? jobFeed.jobs : null}
          activeJobCount={jobFeed.activeCount}
          monitorResults={monitorResults}
          monitorHistory={monitorHistory}
          onClearLeads={async () => { await workspaceApi.clearLeads(workspaceId, user.token); setSavedLeads([]); }}
          onClearHistory={async () => { await workspaceApi.clearHistory(workspaceId, user.token); setHistory([]); setSuggestedComments([]); }}
          onClearMonitorResults={async () => { await workspaceApi.clearMonitorResults(workspaceId, user.token); setMonitorResults([]); }}
          onNavigate={navigate}
        />}
        {activePage === 'Khách hàng tiềm năng' && <LeadsPage leads={savedLeads} suggestions={suggestedComments} onSave={toggleLead} onRemove={(id) => toggleLead({ id })} onClear={async () => { if (window.confirm(t('dashboard.clearLeads'))) { await workspaceApi.clearLeads(workspaceId, user.token); setSavedLeads([]); } }} />}
        {activePage === 'Lịch sử' && <HistoryPage items={history} selectedId={selectedHistoryId} workspaceId={workspaceId} token={user.token} savedIds={savedIds} onToggleSave={toggleLead} onOpen={openHistory} onBack={() => navigate('Lịch sử')} onClear={async () => { if (window.confirm(t('dashboard.clearHistory'))) { await workspaceApi.clearHistory(workspaceId, user.token); setHistory([]); setSuggestedComments([]); navigate('Lịch sử'); } }} />}
        {activePage === 'Tác vụ' && canUse('Extract comment') && <ExtractionJobsPage jobs={jobFeed.jobs} hasMore={jobFeed.hasMore} selectedId={selectedJobId} workspaceId={workspaceId} token={user.token} savedIds={savedIds} onToggleSave={toggleLead} onOpen={openJob} onBack={() => navigate('Tác vụ')} />}
        {activePage === 'Quản trị' && admin && <AdminPage users={users} requests={requests} onDecideUser={onDecideUser} onDecideRequest={onDecideRequest} token={user.token} />}
      </div>
      <footer className="workspace-footer"><span>Leadflow · {workspace.name}</span><span>{t('dashboard.footer')}</span></footer>
    </main>
  </div>;
}

function ToolTabs({ tabs, active, onChange }) {
  return <div className="tool-tabs">{tabs.map(([id, label]) => <button key={id} type="button" className={active === id ? 'active' : ''} onClick={() => onChange(id)}>{label}</button>)}</div>;
}
