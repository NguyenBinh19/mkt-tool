import { useEffect, useState } from 'react';
import { DashboardPage } from '../pages/DashboardPage';
import { AuthPage } from '../pages/AuthPage';
import { WorkspaceHubPage } from '../pages/WorkspaceHubPage';
import { usePersistentState } from '../hooks/usePersistentState';
import { isAdmin } from '../lib/access';
import { activeWorkspaceKey } from '../lib/workspaceStorage';
import { accessApi, authApi, workspaceApi } from '../lib/apiClient';
import { I18nProvider, useI18n } from '../i18n/I18nContext';

export default function App() {
  const [session, setSession] = usePersistentState('leadflow:session', null);
  const [language, setLanguage] = usePersistentState('leadflow:language', 'vi');
  const [theme, setTheme] = usePersistentState('leadflow:theme', 'dark');

  useEffect(() => { document.documentElement.dataset.theme = theme; }, [theme]);

  if (!session) {
    return <I18nProvider language={language} onLanguageChange={setLanguage}><AuthPage onLogin={setSession} theme={theme} onThemeChange={setTheme} /></I18nProvider>;
  }

  return <I18nProvider language={language} onLanguageChange={setLanguage}><WorkspaceApp key={session.id} session={session} onLogout={() => setSession(null)} language={language} onLanguageChange={setLanguage} theme={theme} onThemeChange={setTheme} /></I18nProvider>;
}

function WorkspaceApp({ session, onLogout, language, onLanguageChange, theme, onThemeChange }) {
  const { t } = useI18n();
  const [user, setUser] = useState(null);
  const [users, setUsers] = useState([]);
  const [requests, setRequests] = useState([]);
  const [workspaces, setWorkspaces] = useState([]);
  const [activeWorkspaceId, setActiveWorkspaceId] = usePersistentState(activeWorkspaceKey(session.id), null);
  const [isWorkspaceReady, setIsWorkspaceReady] = useState(false);
  const activeWorkspace = workspaces.find((workspace) => workspace.id === activeWorkspaceId);

  async function refreshAccessData(currentUser = user) {
    if (!currentUser) return;
    const ownRequests = await accessApi.myRequests(currentUser.token);
    if (isAdmin(currentUser)) {
      const [accountData, pendingData] = await Promise.all([accessApi.users(currentUser.token), accessApi.pendingRequests(currentUser.token)]);
      setUsers(accountData);
      setRequests(pendingData);
    } else {
      setUsers([]);
      setRequests(ownRequests);
    }
  }

  useEffect(() => {
    let active = true;
    authApi.me(session.token).then(async (profile) => {
      if (!active) return;
      setUser(profile);
      await Promise.all([
        refreshAccessData(profile),
        workspaceApi.list(profile.token).then((items) => {
          if (!active) return;
          setWorkspaces(items);
          setActiveWorkspaceId((current) => current && items.some((item) => item.id === current) ? current : null);
        })
      ]);
    }).catch(() => { if (active) onLogout(); });
    return () => { active = false; };
  }, [session.token]);

  useEffect(() => {
    if (!activeWorkspace) {
      setIsWorkspaceReady(false);
      return;
    }
    setIsWorkspaceReady(true);
  }, [activeWorkspace?.id, user?.id]);

  async function requestFeature(tool) {
    await accessApi.requestFeature(tool, user.token);
    await refreshAccessData();
  }

  async function decideUser(id, status) {
    await accessApi.decideUser(id, status === 'approved', user.token);
    await refreshAccessData();
  }

  async function decideRequest(request, status) {
    await accessApi.decideRequest(request.id, status === 'approved', user.token);
    await refreshAccessData();
  }

  async function createWorkspace(details) {
    const workspace = await workspaceApi.create(details, user.token);
    setWorkspaces((current) => [workspace, ...current]);
    setActiveWorkspaceId(workspace.id);
  }

  async function updateWorkspace(id, details) {
    const workspace = await workspaceApi.update(id, details, user.token);
    setWorkspaces((current) => current.map((item) => item.id === id ? workspace : item));
  }

  async function deleteWorkspace(id) {
    const nextWorkspace = workspaces.find((workspace) => workspace.id !== id);
    await workspaceApi.remove(id, user.token);
    setWorkspaces((current) => current.filter((workspace) => workspace.id !== id));
    setActiveWorkspaceId(nextWorkspace?.id ?? null);
  }

  if (!user) {
    return <main className="workspace-loading" aria-live="polite">{t('workspace.loading')}</main>;
  }

  if (!activeWorkspace) {
    return <WorkspaceHubPage user={user} workspaces={workspaces} onCreate={createWorkspace} onUpdate={updateWorkspace} onDelete={deleteWorkspace} onOpen={setActiveWorkspaceId} onLogout={onLogout} theme={theme} onThemeChange={onThemeChange} />;
  }

  if (!isWorkspaceReady) {
    return <main className="workspace-loading" aria-live="polite">{t('workspace.loading')}</main>;
  }

  return <DashboardPage user={user} workspace={activeWorkspace} users={users} requests={requests} onRequestFeature={requestFeature} onDecideUser={decideUser} onDecideRequest={decideRequest} onLogout={onLogout} onManageWorkspaces={() => setActiveWorkspaceId(null)} language={language} onLanguageChange={onLanguageChange} theme={theme} onThemeChange={onThemeChange} />;
}
