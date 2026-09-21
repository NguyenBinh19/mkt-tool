import { useMemo, useState } from 'react';
import { CommentItem } from '../features/comments/CommentItem';
import { downloadCommentsCsv } from '../lib/csvExport';

export function LeadsPage({ leads, onRemove, onClear }) {
  const [query, setQuery] = useState('');
  const filtered = useMemo(() => leads.filter((lead) => `${lead.author} ${lead.message ?? ''}`.toLowerCase().includes(query.toLowerCase())), [leads, query]);
  return <section className="page-panel"><div className="panel-head"><div><p className="eyebrow">LEAD WORKSPACE</p><h2>Khách hàng tiềm năng</h2><p className="panel-subtitle">Các cuộc hội thoại đã đánh dấu để đội ngũ chăm sóc sau.</p></div><div className="panel-actions"><button className="export-button" disabled={!filtered.length} onClick={() => downloadCommentsCsv(filtered)}>⇩ Xuất CSV</button><button className="danger-button" disabled={!leads.length} onClick={onClear}>Xoá tất cả</button></div></div>{!leads.length ? <EmptyState icon="☆" title="Chưa có lead được lưu" text="Nhấn ngôi sao ở một comment để đưa người quan tâm vào danh sách này." /> : <><div className="lead-toolbar"><div className="search">⌕<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm trong danh sách lead..." /></div><span>{filtered.length} / {leads.length} lead</span></div><div className="comment-list">{filtered.map((lead) => <CommentItem key={lead.id} comment={lead} isSaved onToggleSave={() => onRemove(lead.id)} />)}</div></>}</section>;
}

export function EmptyState({ icon, title, text }) { return <div className="empty"><div>{icon}</div><h3>{title}</h3><p>{text}</p></div>; }
