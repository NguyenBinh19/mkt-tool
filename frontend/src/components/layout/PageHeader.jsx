import { Icon } from '../ui/Icon';
import { isAdmin } from '../../lib/access';
import { useI18n } from '../../i18n/I18nContext';

export function PageHeader({ activePage, workspace, isLoading, onOpenSession, onMenu, menuOpen, user }) {
  const { t } = useI18n();
  const titles = { 'Kho công cụ': t('header.workspace'), 'Extract comment': t('header.extract'), 'Content monitor': t('header.monitor'), 'Dữ liệu workspace': t('header.data'), 'Lead scoring': t('header.signals'), 'Khách hàng tiềm năng': t('header.leads'), 'Lịch sử': t('header.history'), 'Quản trị': t('header.admin') };
  return <header className="workspace-header">
    <div className="header-title">
      <button className="mobile-menu-button" aria-label={t('header.openMenu')} aria-expanded={menuOpen} aria-controls="workspace-navigation" onClick={onMenu}><Icon name="menu" /></button>
      <div><p className="eyebrow">{workspace?.name?.toLocaleUpperCase() ?? 'WORKSPACE'} <span aria-hidden="true">/</span> LEADFLOW</p><h1>{titles[activePage] ?? activePage}</h1></div>
    </div>
    {activePage === 'Extract comment'
      ? <button className="ghost-button" onClick={onOpenSession} disabled={isLoading}><Icon name="chat" size={16} /> {t('header.openFacebook')}</button>
      : <div className="header-account"><span className="role-chip"><Icon name={isAdmin(user) ? 'shield' : 'users'} size={14} />{isAdmin(user) ? t('nav.adminRole') : t('nav.member')}</span><div className="avatar">{user.name.slice(0, 1).toUpperCase()}</div></div>}
  </header>;
}
