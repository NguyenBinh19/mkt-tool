export const isAdmin = (user) => String(user?.role).toUpperCase() === 'ADMIN';
export const isPending = (item) => String(item?.status).toUpperCase() === 'PENDING';
export const canUseTool = (user, tool) => isAdmin(user) || Boolean(user?.permissions?.includes(tool));
