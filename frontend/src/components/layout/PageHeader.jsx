import { Icon } from '../ui/Icon';
import { isAdmin } from '../../lib/access';

const titles = { 'Kho công cụ': 'Không gian làm việc', 'Lead scoring': 'Tín hiệu quan tâm', 'Khách hàng tiềm năng': 'Lead đã lưu', 'Quản trị': 'Quản trị & phê duyệt' };

export function PageHeader({ activePage, isLoading, onOpenSession, onMenu, menuOpen, user }) {
  return <header className="workspace-header">
    <div className="header-title">
      <button className="mobile-menu-button" aria-label="Mở menu" aria-expanded={menuOpen} aria-controls="workspace-navigation" onClick={onMenu}><Icon name="menu" /></button>
      <div><p className="eyebrow">LEADFLOW <span aria-hidden="true">/</span> WORKSPACE</p><h1>{titles[activePage] ?? activePage}</h1></div>
    </div>
    {activePage === 'Extract comment'
      ? <button className="ghost-button" onClick={onOpenSession} disabled={isLoading}><Icon name="chat" size={16} /> Mở phiên Facebook</button>
      : <div className="header-account"><span className="role-chip"><Icon name={isAdmin(user) ? 'shield' : 'users'} size={14} />{isAdmin(user) ? 'Quản trị viên' : 'Thành viên'}</span><div className="avatar">{user.name.slice(0, 1).toUpperCase()}</div></div>}
  </header>;
}
