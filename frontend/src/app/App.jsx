import { useEffect, useState } from 'react';
import { DashboardPage } from '../pages/DashboardPage';
import { AuthPage } from '../pages/AuthPage';
import { WorkspaceHubPage } from '../pages/WorkspaceHubPage';
import { usePersistentState } from '../hooks/usePersistentState';
import { isPending } from '../lib/access';
import { activeWorkspaceKey } from '../lib/workspaceStorage';
import { workspaceApi } from '../lib/apiClient';
import { I18nProvider, useI18n } from '../i18n/I18nContext';

const INITIAL_USERS = [];

export default function App() {
  const [users, setUsers] = usePersistentState('leadflow:users', INITIAL_USERS);
  const [featureRequests, setFeatureRequests] = usePersistentState('leadflow:feature-requests', []);
  const [session, setSession] = usePersistentState('leadflow:session', null);
  const [language, setLanguage] = usePersistentState('leadflow:language', 'vi');
  const [theme, setTheme] = usePersistentState('leadflow:theme', 'dark');

  useEffect(() => { document.documentElement.dataset.theme = theme; }, [theme]);

  if (!session) {
    return <I18nProvider language={language} onLanguageChange={setLanguage}><AuthPage onLogin={setSession} theme={theme} onThemeChange={setTheme} /></I18nProvider>;
  }

  const currentUser = users.find((user) => user.id === session.id) ?? session;
  function decideUser(id, status) {
    setUsers((current) => current.map((user) => user.id === id ? { ...user, status } : user));
  }
  function decideRequest(request, status) {
    setFeatureRequests((current) => current.map((item) => item.id === request.id ? { ...item, status } : item));
    if (status === 'approved') {
      setUsers((current) => current.map((user) => user.id === request.userId
        ? { ...user, permissions: [...new Set([...(user.permissions ?? []), request.tool])] }
        : user));
    }
  }
  function requestFeature(tool) {
    setFeatureRequests((current) => current.some((request) => request.userId === currentUser.id && request.tool === tool && isPending(request))
      ? current
      : [...current, { id: crypto.randomUUID(), userId: currentUser.id, userName: currentUser.name, tool, status: 'pending', createdAt: new Date().toISOString() }]);
  }

  return <I18nProvider language={language} onLanguageChange={setLanguage}><WorkspaceApp key={currentUser.id} user={currentUser} users={users} requests={featureRequests} onRequestFeature={requestFeature} onDecideUser={decideUser} onDecideRequest={decideRequest} onLogout={() => setSession(null)} language={language} onLanguageChange={setLanguage} theme={theme} onThemeChange={setTheme} /></I18nProvider>;
}

function WorkspaceApp({ user, users, requests, onRequestFeature, onDecideUser, onDecideRequest, onLogout, language, onLanguageChange, theme, onThemeChange }) {
  const { t } = useI18n();
  const [workspaces, setWorkspaces] = useState([]);
  const [activeWorkspaceId, setActiveWorkspaceId] = usePersistentState(activeWorkspaceKey(user.id), null);
  const [isWorkspaceReady, setIsWorkspaceReady] = useState(false);
  const activeWorkspace = workspaces.find((workspace) => workspace.id === activeWorkspaceId);

  useEffect(() => {
    workspaceApi.list(user.token).then((items) => {
      setWorkspaces(items);
      setActiveWorkspaceId((current) => current && items.some((item) => item.id === current) ? current : null);
    }).catch(() => onLogout());
  }, [user.id, user.token]);

  useEffect(() => {
    if (!activeWorkspace) {
      setIsWorkspaceReady(false);
      return;
    }
    setIsWorkspaceReady(true);
  }, [activeWorkspace?.id, user.id]);

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

  if (!activeWorkspace) {
    return <WorkspaceHubPage user={user} workspaces={workspaces} onCreate={createWorkspace} onUpdate={updateWorkspace} onDelete={deleteWorkspace} onOpen={setActiveWorkspaceId} onLogout={onLogout} theme={theme} onThemeChange={onThemeChange} />;
  }

  if (!isWorkspaceReady) {
    return <main className="workspace-loading" aria-live="polite">{t('workspace.loading')}</main>;
  }

  return <DashboardPage user={user} workspace={activeWorkspace} users={users} requests={requests} onRequestFeature={onRequestFeature} onDecideUser={onDecideUser} onDecideRequest={onDecideRequest} onLogout={onLogout} onManageWorkspaces={() => setActiveWorkspaceId(null)} language={language} onLanguageChange={onLanguageChange} theme={theme} onThemeChange={onThemeChange} />;
}
