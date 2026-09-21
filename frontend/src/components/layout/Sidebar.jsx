import { Icon } from '../ui/Icon';
import { isAdmin } from '../../lib/access';

const navigation = [
  { name: 'Kho công cụ', icon: 'grid', label: 'Kho công cụ' },
  { name: 'Extract comment', icon: 'chat', label: 'Extract comment' },
  { name: 'Lead scoring', icon: 'target', label: 'Tín hiệu quan tâm' },
  { name: 'Khách hàng tiềm năng', icon: 'users', label: 'Lead đã lưu' },
  { name: 'Lịch sử', icon: 'clock', label: 'Lịch sử hoạt động' },
];

export function Sidebar({ activePage, onNavigate, leadCount, pendingCount, isOpen, onClose, onLogout, user }) {
  function navigate(page) { onNavigate(page); onClose(); }
  function item(page, label, icon, count) {
    return <button key={page} type="button" className={activePage === page ? 'nav-item active' : 'nav-item'} aria-current={activePage === page ? 'page' : undefined} onClick={() => navigate(page)}>
      <Icon name={icon} /><span className="nav-label">{label}</span>{count > 0 && <b className="nav-count">{count}</b>}
    </button>;
  }
  return <aside id="workspace-navigation" className={`sidebar${isOpen ? ' sidebar-open' : ''}`}>
    <button className="sidebar-close" aria-label="Đóng menu" onClick={onClose}><Icon name="close" /></button>
    <button className="brand brand-button" onClick={() => navigate('Kho công cụ')} aria-label="Leadflow — Kho công cụ">
      <span className="brand-mark">↗</span><span>lead<span>flow</span></span>
    </button>
    <div className="workspace"><span className="workspace-icon"><Icon name="grid" /></span><div><strong>Marketing workspace</strong><small>Không gian tăng trưởng</small></div></div>
    <p className="nav-section-label">WORKSPACE</p>
    <nav aria-label="Điều hướng workspace">
      {navigation.map(({ name, label, icon }) => item(name, label, icon, name === 'Khách hàng tiềm năng' ? leadCount : 0))}
    </nav>
    {isAdmin(user) && <><p className="nav-section-label management-label">QUẢN LÝ</p><nav aria-label="Điều hướng quản trị">{item('Quản trị', 'Quản trị & phê duyệt', 'shield', pendingCount)}</nav></>}
    <div className="sidebar-bottom">
      <div className="sidebar-tip"><Icon name="target" /><strong>Từ hội thoại đến cơ hội.</strong><p>Thu thập tín hiệu. Lưu người quan tâm. Chăm sóc đúng lúc.</p></div>
      <div className="profile"><div className="avatar">{(user?.name ?? 'U').slice(0, 1).toUpperCase()}</div><div><strong>{user?.name}</strong><small>{isAdmin(user) ? 'Quản trị viên' : 'Thành viên'}</small></div><button aria-label="Đăng xuất" title="Đăng xuất" onClick={onLogout}><Icon name="logout" size={18} /></button></div>
    </div>
  </aside>;
}
