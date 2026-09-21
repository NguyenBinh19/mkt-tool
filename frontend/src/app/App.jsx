import { DashboardPage } from '../pages/DashboardPage';
import { AuthPage } from '../pages/AuthPage';
import { usePersistentState } from '../hooks/usePersistentState';
import { isPending } from '../lib/access';

const INITIAL_USERS = [{ id: 'admin', name: 'Admin', email: 'admin@leadflow.local', password: 'admin123', role: 'admin', status: 'approved', createdAt: new Date().toISOString() }];

export default function App() {
  const [users, setUsers] = usePersistentState('leadflow:users', INITIAL_USERS);
  const [featureRequests, setFeatureRequests] = usePersistentState('leadflow:feature-requests', []);
  const [session, setSession] = usePersistentState('leadflow:session', null);

  if (!session) {
    return <AuthPage users={users} onLogin={setSession} onRegister={(user) => setUsers((current) => [...current, user])} />;
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

  return (
    <DashboardPage
      key={currentUser.id}
      user={currentUser}
      users={users}
      requests={featureRequests}
      onRequestFeature={requestFeature}
      onDecideUser={decideUser}
      onDecideRequest={decideRequest}
      onLogout={() => setSession(null)}
    />
  );
}
