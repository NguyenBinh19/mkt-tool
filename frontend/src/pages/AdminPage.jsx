import { useState } from 'react';
import { Icon } from '../components/ui/Icon';
import { isPending } from '../lib/access';

export function AdminPage({ users, requests, onDecideUser, onDecideRequest }) {
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
    setFeedback(status === 'approved' ? 'Đã phê duyệt yêu cầu.' : 'Đã từ chối yêu cầu.');
  }

  return <section className="approval-page">
    <div className="section-introduction"><span className="section-symbol"><Icon name="shield" size={24} /></span><div><h2>Đúng người, đúng quyền truy cập.</h2><p>Xét duyệt thành viên và mở quyền sử dụng công cụ cho đội ngũ của bạn.</p></div></div>
    <div className="overview-stats">
      <div><Icon name="users" /><span>Thành viên chờ duyệt</span><strong>{pendingUsers.length}</strong></div>
      <div><Icon name="shield" /><span>Yêu cầu công cụ</span><strong>{pendingRequests.length}</strong></div>
      <div><Icon name="check" /><span>Tài khoản đã duyệt</span><strong>{users.filter((user) => String(user.status).toUpperCase() === 'APPROVED').length}</strong></div>
    </div>
    <section className="approval-panel">
      <div className="approval-toolbar">
        <div className="segmented-control" aria-label="Loại yêu cầu">
          <button aria-pressed={tab === 'features'} onClick={() => { setTab('features'); setQuery(''); }}>Quyền công cụ <b>{pendingRequests.length}</b></button>
          <button aria-pressed={tab === 'users'} onClick={() => { setTab('users'); setQuery(''); }}>Tài khoản <b>{pendingUsers.length}</b></button>
        </div>
        <label className="catalog-search"><Icon name="search" size={18} /><input aria-label="Tìm yêu cầu" placeholder="Tìm yêu cầu…" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
      </div>
      <p className="action-feedback" role="status">{feedback}</p>
      {items.length === 0 ? <div className="approval-empty">
        <span><Icon name={query ? 'search' : 'check'} size={30} /></span>
        <h3>{query ? 'Không tìm thấy yêu cầu' : 'Bạn đã xử lý hết yêu cầu'}</h3>
        <p>{query ? 'Thử tìm bằng tên, email hoặc tên công cụ khác.' : 'Yêu cầu mới sẽ xuất hiện tại đây khi thành viên gửi đăng ký.'}</p>
      </div> : <div className="approval-list">{items.map((item) => <article className="approval-row" key={item.id}>
        <div className="avatar">{(item.name ?? item.userName ?? 'U').slice(0, 1).toUpperCase()}</div>
        <div className="approval-main"><strong>{tab === 'features' ? item.tool : item.name}</strong><small>{tab === 'features' ? item.userName : item.email}</small></div>
        <span className="status-badge pending">Chờ duyệt</span>
        <div className="approval-actions"><button className="ghost-button" onClick={() => decide(item, 'rejected')}>Từ chối</button><button className="primary-button approval-button" onClick={() => decide(item, 'approved')}><Icon name="check" size={16} /> Phê duyệt</button></div>
      </article>)}</div>}
    </section>
  </section>;
}
