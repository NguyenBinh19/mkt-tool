import { useEffect, useRef, useState } from 'react';
import { PageHeader } from '../components/layout/PageHeader';
import { Sidebar } from '../components/layout/Sidebar';
import { CommentsPanel } from '../features/comments/CommentsPanel';
import { ExtractionForm } from '../features/extraction/ExtractionForm';
import { ExtractionHero } from '../features/extraction/ExtractionHero';
import { useFacebookExtraction } from '../hooks/useFacebookExtraction';
import { canUseTool, isAdmin, isPending } from '../lib/access';
import { workspaceApi } from '../lib/apiClient';
import { LeadsPage } from './LeadsPage';
import { HistoryPage } from './HistoryPage';
import { ToolsPage } from './ToolsPage';
import { AdminPage } from './AdminPage';
import { LeadSignalsPage } from './LeadSignalsPage';
import { ContentMonitorPage } from './ContentMonitorPage';
import { DataCenterPage } from './DataCenterPage';
import { useI18n } from '../i18n/I18nContext';

const routes = {
  'Kho công cụ': '#/tools',
  'Extract comment': '#/tools/extract',
  'Lead scoring': '#/tools/extract/signals',
  'Content monitor': '#/tools/content',
  'Khách hàng tiềm năng': '#/data/leads',
  'Dữ liệu workspace': '#/data',
  'Lịch sử': '#/history',
  'Quản trị': '#/admin'
};

function pageFromRoute(hash) {
  if (hash === '#/tools/extract/signals') return 'Lead scoring';
  if (hash === '#/tools/content/following') return 'Content monitor';
  return Object.entries(routes).find(([, route]) => route === hash)?.[0] ?? 'Kho công cụ';
}

export function DashboardPage({ user, workspace, users = [], requests = [], onRequestFeature, onDecideUser, onDecideRequest, onLogout, onManageWorkspaces, language, onLanguageChange, theme, onThemeChange }) {
  const [activePage, setActivePage] = useState(() => pageFromRoute(window.location.hash));
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const workspaceId = workspace.id;
  const [savedLeads, setSavedLeads] = useState([]);
  const [history, setHistory] = useState([]);
  const [monitorResults, setMonitorResults] = useState([]);
  const [monitorHistory, setMonitorHistory] = useState([]);
  const [extractView, setExtractView] = useState('extract');
  const [monitorView, setMonitorView] = useState('search');
  const { t } = useI18n();
  const mainRef = useRef(null);
  const extraction = useFacebookExtraction(user.id, workspaceId);
  const comments = extraction.result?.comments ?? [];
  const savedIds = new Set(savedLeads.map((lead) => lead.id));
  const admin = isAdmin(user);
  const pendingCount = users.filter(isPending).length + requests.filter(isPending).length;
  const canUse = (tool) => canUseTool(user, tool);
  const requested = (tool) => requests.some((request) => request.userId === user.id && request.tool === tool && isPending(request));

  useEffect(() => {
    if (!sidebarOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const close = (event) => { if (event.key === 'Escape') setSidebarOpen(false); };
    window.addEventListener('keydown', close);
    return () => { document.body.style.overflow = previousOverflow; window.removeEventListener('keydown', close); };
  }, [sidebarOpen]);

  useEffect(() => {
    const syncRoute = () => setActivePage(pageFromRoute(window.location.hash));
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
    Promise.all([workspaceApi.leads(workspaceId, user.token), workspaceApi.history(workspaceId, user.token), workspaceApi.monitor(workspaceId, user.token)])
      .then(([leadData, historyData, monitor]) => {
        setSavedLeads(leadData.map((lead) => ({ ...lead, id: lead.externalCommentId, recordId: lead.id, parentId: lead.parentCommentId, createdAt: lead.commentCreatedAt })));
        setHistory(historyData.map((item) => ({ ...item, url: item.postUrl, count: item.commentCount })));
        setMonitorResults(monitor.results);
        setMonitorHistory(monitor.history);
      }).catch(() => onLogout());
  }, [workspaceId, user.token]);

  function navigate(page) {
    if (page === 'Quản trị' && !admin) return;
    if (['Extract comment', 'Lead scoring', 'Content monitor'].includes(page) && !canUse(page)) page = 'Kho công cụ';
    const targetRoute = routes[page] ?? routes['Kho công cụ'];
    if (window.location.hash !== targetRoute) window.location.hash = targetRoute;
    setActivePage(page);
    if (page === 'Lead scoring') setExtractView('signals');
    if (page === 'Extract comment') setExtractView('extract');
    setSidebarOpen(false);
    mainRef.current?.focus();
    window.scrollTo({ top: 0, behavior: 'auto' });
  }

  async function runExtraction() {
    if (!canUse('Extract comment')) return;
    const submittedUrl = extraction.form.postUrl;
    const data = await extraction.extractComments();
    if (data?.status === 'ok') {
      const item = await workspaceApi.addHistory(workspaceId, { postUrl: submittedUrl, commentCount: data.comments?.length ?? 0 }, user.token);
      setHistory((current) => [{ ...item, url: item.postUrl, count: item.commentCount }, ...current].slice(0, 30));
    }
  }

  async function toggleLead(comment) {
    const existing = savedLeads.find((lead) => lead.id === comment.id);
    if (existing) {
      await workspaceApi.removeLead(workspaceId, existing.recordId, user.token);
      setSavedLeads((current) => current.filter((lead) => lead.id !== comment.id));
      return;
    }
    const lead = await workspaceApi.saveLead(workspaceId, { externalCommentId: comment.id, author: comment.author, authorUrl: comment.authorUrl, message: comment.message, parentCommentId: comment.parentId, commentCreatedAt: comment.createdAt }, user.token);
    setSavedLeads((current) => [{ ...lead, id: lead.externalCommentId, recordId: lead.id, parentId: lead.parentCommentId, createdAt: lead.commentCreatedAt }, ...current]);
  }

  return <div className="shell workspace-shell">
    <a className="skip-link" href="#workspace-main">{t('common.skipContent')}</a>
    {sidebarOpen && <button className="sidebar-backdrop" aria-label={t('common.closeMenu')} onClick={() => setSidebarOpen(false)} />}
    <Sidebar activePage={activePage} onNavigate={navigate} pendingCount={pendingCount} isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} onLogout={onLogout} user={user} workspace={workspace} onManageWorkspaces={onManageWorkspaces} language={language} onLanguageChange={onLanguageChange} theme={theme} onThemeChange={onThemeChange} />
    <main id="workspace-main" ref={mainRef} tabIndex={-1} className="content">
      <PageHeader activePage={activePage} workspace={workspace} isLoading={extraction.isOpening} onOpenSession={extraction.openFacebookSession} onMenu={() => setSidebarOpen(true)} menuOpen={sidebarOpen} user={user} />
      {extraction.job?.status === 'running' && <div className="extraction-activity running"><span className="activity-pulse" />{t('dashboard.extractRunning')}</div>}
      {extraction.job?.status === 'completed' && activePage !== 'Extract comment' && <div className="extraction-activity completed">✓ {t('dashboard.extractDone', { count: extraction.job.count })}</div>}
      {extraction.job?.status === 'failed' && activePage !== 'Extract comment' && <div className="extraction-activity failed">! {t('dashboard.extractFailed')}</div>}
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
        {activePage === 'Dữ liệu workspace' && <DataCenterPage workspace={workspace} comments={comments} leads={savedLeads} history={history} monitorResults={monitorResults} monitorHistory={monitorHistory} onClearComments={extraction.clearResult} onClearLeads={async () => { await workspaceApi.clearLeads(workspaceId, user.token); setSavedLeads([]); }} onClearHistory={async () => { await workspaceApi.clearHistory(workspaceId, user.token); setHistory([]); }} onClearMonitorResults={async () => { await workspaceApi.clearMonitorResults(workspaceId, user.token); setMonitorResults([]); }} onNavigate={navigate} />}
        {activePage === 'Khách hàng tiềm năng' && <LeadsPage leads={savedLeads} onRemove={(id) => toggleLead({ id })} onClear={async () => { if (window.confirm(t('dashboard.clearLeads'))) { await workspaceApi.clearLeads(workspaceId, user.token); setSavedLeads([]); } }} />}
        {activePage === 'Lịch sử' && <HistoryPage items={history} onClear={async () => { if (window.confirm(t('dashboard.clearHistory'))) { await workspaceApi.clearHistory(workspaceId, user.token); setHistory([]); } }} />}
        {activePage === 'Quản trị' && admin && <AdminPage users={users} requests={requests} onDecideUser={onDecideUser} onDecideRequest={onDecideRequest} />}
      </div>
      <footer className="workspace-footer"><span>Leadflow · {workspace.name}</span><span>{t('dashboard.footer')}</span></footer>
    </main>
  </div>;
}

function ToolTabs({ tabs, active, onChange }) {
  return <div className="tool-tabs">{tabs.map(([id, label]) => <button key={id} type="button" className={active === id ? 'active' : ''} onClick={() => onChange(id)}>{label}</button>)}</div>;
}
