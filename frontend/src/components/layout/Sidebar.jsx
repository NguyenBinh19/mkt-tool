import { Icon } from '../ui/Icon';
import { isAdmin } from '../../lib/access';
import { useI18n } from '../../i18n/I18nContext';
import { PreferencesControl } from '../ui/PreferencesControl';

const navigation = [
  { name: 'Kho công cụ', icon: 'grid', label: 'Kho công cụ' },
  { name: 'Dữ liệu workspace', icon: 'chart', label: 'Dữ liệu & kết quả' },
  { name: 'Lịch sử', icon: 'clock', label: 'Lịch sử hoạt động' },
];
export function Sidebar({ activePage, onNavigate, pendingCount, isOpen, onClose, onLogout, user, workspace, onManageWorkspaces, theme, onThemeChange }) {
  const { t } = useI18n();
  function navigate(page) { onNavigate(page); onClose(); }
  function item(page, label, icon, count) {
    return <button key={page} type="button" className={activePage === page ? 'nav-item active' : 'nav-item'} aria-current={activePage === page ? 'page' : undefined} onClick={() => navigate(page)}>
      <Icon name={icon} /><span className="nav-label">{label}</span>{count > 0 && <b className="nav-count">{count}</b>}
    </button>;
  }
  return <aside id="workspace-navigation" className={`sidebar${isOpen ? ' sidebar-open' : ''}`}>
    <button className="sidebar-close" aria-label={t('common.close')} onClick={onClose}><Icon name="close" /></button>
    <button className="brand brand-button" onClick={() => navigate('Kho công cụ')} aria-label={`Leadflow — ${t('nav.tools')}`}>
      <span className="brand-mark">↗</span><span>lead<span>flow</span></span>
    </button>
    <div className="workspace workspace-summary"><span className="workspace-icon"><Icon name="grid" /></span><div><strong>{workspace.name}</strong><small>{workspace.industry}</small></div><button type="button" onClick={onManageWorkspaces} title={t('nav.manageWorkspace')} aria-label={t('nav.manageWorkspace')}>•••</button></div>
    <p className="nav-section-label">{t('nav.workspace')}</p>
    <nav aria-label={t('nav.workspace')}>
      {navigation.map(({ name, icon }) => item(name, name === 'Kho công cụ' ? t('nav.tools') : name === 'Dữ liệu workspace' ? t('nav.data') : t('nav.history'), icon, 0))}
    </nav>
    {isAdmin(user) && <><p className="nav-section-label management-label">{t('nav.management')}</p><nav>{item('Quản trị', t('nav.admin'), 'shield', pendingCount)}</nav></>}
    <div className="sidebar-bottom">
      <PreferencesControl compact theme={theme} onThemeChange={onThemeChange} />
      <div className="sidebar-tip"><Icon name="target" /><strong>{t('nav.tipTitle')}</strong><p>{t('nav.tipText')}</p></div>
      <div className="profile"><div className="avatar">{(user?.name ?? 'U').slice(0, 1).toUpperCase()}</div><div><strong>{user?.name}</strong><small>{isAdmin(user) ? t('nav.adminRole') : t('nav.member')}</small></div><button aria-label={t('common.logout')} title={t('common.logout')} onClick={onLogout}><Icon name="logout" size={18} /></button></div>
    </div>
  </aside>;
}
