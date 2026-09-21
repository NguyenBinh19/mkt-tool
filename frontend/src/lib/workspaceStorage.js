export const DEFAULT_WORKSPACE = {
  id: 'default',
  name: 'Marketing workspace',
  industry: 'Không gian tăng trưởng',
  description: 'Theo dõi các tín hiệu thị trường và cơ hội tiếp cận đầu tiên.',
  createdAt: new Date(0).toISOString()
};

export function workspaceKey(userId, workspaceId, resource) {
  return `leadflow:${userId}:${workspaceId}:${resource}`;
}

export function workspaceListKey(userId) {
  return `leadflow:${userId}:workspaces`;
}

export function activeWorkspaceKey(userId) {
  return `leadflow:${userId}:active-workspace`;
}

export function removeWorkspaceData(userId, workspaceId) {
  ['saved-leads', 'history', 'extraction-form', 'extraction-result', 'content-monitor-results', 'content-monitor-history']
    .forEach((resource) => localStorage.removeItem(workspaceKey(userId, workspaceId, resource)));
}

export function migrateLegacyWorkspaceData(userId, workspaceId) {
  const legacyResources = ['extraction-form', 'extraction-result', 'content-monitor-results', 'content-monitor-history'];
  legacyResources.forEach((resource) => {
    const targetKey = workspaceKey(userId, workspaceId, resource);
    const legacyKey = `leadflow:${workspaceId}:${resource}`;
    if (!localStorage.getItem(targetKey) && localStorage.getItem(legacyKey)) {
      localStorage.setItem(targetKey, localStorage.getItem(legacyKey));
      localStorage.removeItem(legacyKey);
    }
  });
}
