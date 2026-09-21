import { useEffect, useRef, useState } from 'react';
import { PageHeader } from '../components/layout/PageHeader';
import { Sidebar } from '../components/layout/Sidebar';
import { CommentsPanel } from '../features/comments/CommentsPanel';
import { ExtractionForm } from '../features/extraction/ExtractionForm';
import { ExtractionHero } from '../features/extraction/ExtractionHero';
import { useFacebookExtraction } from '../hooks/useFacebookExtraction';
import { usePersistentState } from '../hooks/usePersistentState';
import { canUseTool, isAdmin, isPending } from '../lib/access';
import { LeadsPage } from './LeadsPage';
import { HistoryPage } from './HistoryPage';
import { ToolsPage } from './ToolsPage';
import { AdminPage } from './AdminPage';
import { LeadSignalsPage } from './LeadSignalsPage';

export function DashboardPage({ user, users = [], requests = [], onRequestFeature, onDecideUser, onDecideRequest, onLogout }) {
  const [activePage, setActivePage] = useState('Kho công cụ');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [savedLeads, setSavedLeads] = usePersistentState('leadflow:saved-leads', []);
  const [history, setHistory] = usePersistentState('leadflow:history', []);
  const mainRef = useRef(null);
  const extraction = useFacebookExtraction();
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

  function navigate(page) {
    if (page === 'Quản trị' && !admin) return;
    if (['Extract comment', 'Lead scoring'].includes(page) && !canUse(page)) page = 'Kho công cụ';
    setActivePage(page);
    setSidebarOpen(false);
    mainRef.current?.focus();
    window.scrollTo({ top: 0, behavior: 'instant' });
  }

  async function runExtraction() {
    if (!canUse('Extract comment')) return;
    const data = await extraction.extractComments();
    if (data?.status === 'ok') {
      setHistory((current) => [{ id: crypto.randomUUID(), url: extraction.form.postUrl, count: data.comments?.length ?? 0, createdAt: new Date().toISOString() }, ...current].slice(0, 30));
    }
  }

  function toggleLead(comment) {
    setSavedLeads((current) => current.some((lead) => lead.id === comment.id)
      ? current.filter((lead) => lead.id !== comment.id)
      : [{ ...comment, savedAt: new Date().toISOString() }, ...current]);
  }

  return <div className="shell workspace-shell">
    <a className="skip-link" href="#workspace-main">Đến nội dung chính</a>
    {sidebarOpen && <button className="sidebar-backdrop" aria-label="Đóng menu điều hướng" onClick={() => setSidebarOpen(false)} />}
    <Sidebar activePage={activePage} onNavigate={navigate} leadCount={savedLeads.length} pendingCount={pendingCount} isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} onLogout={onLogout} user={user} />
    <main id="workspace-main" ref={mainRef} tabIndex={-1} className="content">
      <PageHeader activePage={activePage} isLoading={extraction.isLoading} onOpenSession={extraction.openFacebookSession} onMenu={() => setSidebarOpen(true)} menuOpen={sidebarOpen} user={user} />
      <div className="workspace-page" key={activePage}>
        {activePage === 'Kho công cụ' && <ToolsPage user={user} onOpenTool={navigate} onRequestTool={onRequestFeature} canUse={canUse} requested={requested} resultCount={comments.length} leadCount={savedLeads.length} historyCount={history.length} />}
        {activePage === 'Extract comment' && canUse('Extract comment') && <>
          <ExtractionHero />
          <ExtractionForm form={extraction.form} isLoading={extraction.isLoading} notice={extraction.notice} onChange={extraction.updateField} onSubmit={runExtraction} />
          <CommentsPanel comments={comments} savedIds={savedIds} onToggleSave={toggleLead} />
        </>}
        {activePage === 'Lead scoring' && canUse('Lead scoring') && <LeadSignalsPage comments={comments} savedIds={savedIds} onToggleSave={toggleLead} onExtract={() => navigate('Extract comment')} canExtract={canUse('Extract comment')} />}
        {activePage === 'Khách hàng tiềm năng' && <LeadsPage leads={savedLeads} onRemove={(id) => setSavedLeads((current) => current.filter((lead) => lead.id !== id))} onClear={() => { if (window.confirm('Xóa toàn bộ lead đã lưu?')) setSavedLeads([]); }} />}
        {activePage === 'Lịch sử' && <HistoryPage items={history} onClear={() => { if (window.confirm('Xóa toàn bộ lịch sử hoạt động?')) setHistory([]); }} />}
        {activePage === 'Quản trị' && admin && <AdminPage users={users} requests={requests} onDecideUser={onDecideUser} onDecideRequest={onDecideRequest} />}
      </div>
      <footer className="workspace-footer"><span>Leadflow · Marketing workspace</span><span>Từng tín hiệu. Thêm cơ hội.</span></footer>
    </main>
  </div>;
}
