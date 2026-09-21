import { useState } from 'react';
import { Icon } from '../components/ui/Icon';
import { isPending } from '../lib/access';
import { useI18n } from '../i18n/I18nContext';

export function AdminPage({ users, requests, onDecideUser, onDecideRequest }) {
  const { t } = useI18n();
  const [tab, setTab] = useState('features');
  const [query, setQuery] = useState('');
  const [feedback, setFeedback] = useState('');
  const pendingUsers = users.filter(isPending);
  const pendingRequests = requests.filter(isPending);
  const items = (tab === 'features' ? pendingRequests : pendingUsers).filter((item) =>
    [item.name, item.email, item.userName, item.tool].join(' ').toLocaleLowerCase().includes(query.toLocaleLowerCase()));
  function decide(item, status) {
    if (tab === 'features') onDecideRequest(item, status);
    else onDecideUser(item.id, status);
    setFeedback(status === 'approved' ? t('admin.approved') : t('admin.rejected'));
  }

  return <section className="approval-page">
    <div className="section-introduction"><span className="section-symbol"><Icon name="shield" size={24} /></span><div><h2>{t('admin.title')}</h2><p>{t('admin.text')}</p></div></div>
    <div className="overview-stats">
      <div><Icon name="users" /><span>{t('admin.pendingUsers')}</span><strong>{pendingUsers.length}</strong></div>
      <div><Icon name="shield" /><span>{t('admin.toolRequests')}</span><strong>{pendingRequests.length}</strong></div>
      <div><Icon name="check" /><span>{t('admin.approvedUsers')}</span><strong>{users.filter((user) => String(user.status).toUpperCase() === 'APPROVED').length}</strong></div>
    </div>
    <section className="approval-panel">
      <div className="approval-toolbar">
        <div className="segmented-control" aria-label={t('admin.requestType')}>
          <button aria-pressed={tab === 'features'} onClick={() => { setTab('features'); setQuery(''); }}>{t('admin.toolAccess')} <b>{pendingRequests.length}</b></button>
          <button aria-pressed={tab === 'users'} onClick={() => { setTab('users'); setQuery(''); }}>{t('admin.accounts')} <b>{pendingUsers.length}</b></button>
        </div>
        <label className="catalog-search"><Icon name="search" size={18} /><input aria-label={t('admin.searchLabel')} placeholder={t('admin.search')} value={query} onChange={(event) => setQuery(event.target.value)} /></label>
      </div>
      <p className="action-feedback" role="status">{feedback}</p>
      {items.length === 0 ? <div className="approval-empty">
        <span><Icon name={query ? 'search' : 'check'} size={30} /></span>
        <h3>{query ? t('admin.noMatch') : t('admin.allDone')}</h3>
        <p>{query ? t('admin.noMatchText') : t('admin.allDoneText')}</p>
      </div> : <div className="approval-list">{items.map((item) => <article className="approval-row" key={item.id}>
        <div className="avatar">{(item.name ?? item.userName ?? 'U').slice(0, 1).toUpperCase()}</div>
        <div className="approval-main"><strong>{tab === 'features' ? item.tool : item.name}</strong><small>{tab === 'features' ? item.userName : item.email}</small></div>
        <span className="status-badge pending">{t('admin.pending')}</span>
        <div className="approval-actions"><button className="ghost-button" onClick={() => decide(item, 'rejected')}>{t('admin.reject')}</button><button className="primary-button approval-button" onClick={() => decide(item, 'approved')}><Icon name="check" size={16} /> {t('admin.approve')}</button></div>
      </article>)}</div>}
    </section>
  </section>;
}
