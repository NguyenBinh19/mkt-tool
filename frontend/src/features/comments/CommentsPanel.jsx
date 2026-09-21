import { useMemo, useState } from 'react';
import { downloadCommentsCsv } from '../../lib/csvExport';
import { isPotentialLead, summarizeComments } from '../../lib/leadScoring';
import { CommentItem } from './CommentItem';
import { StatCard } from './StatCard';

export function CommentsPanel({ comments, savedIds, onToggleSave }) {
  const [query, setQuery] = useState('');
  const [onlyLeads, setOnlyLeads] = useState(false);
  const summary = summarizeComments(comments);
  const visibleComments = useMemo(() => comments.filter((comment) => {
    const content = `${comment.author} ${comment.message ?? ''}`.toLocaleLowerCase();
    return (!query || content.includes(query.toLocaleLowerCase())) && (!onlyLeads || isPotentialLead(comment));
  }), [comments, onlyLeads, query]);

  return <section className="results-section"><div className="section-title"><div><p className="eyebrow">KẾT QUẢ GẦN NHẤT</p><h2>Luồng hội thoại</h2></div>{comments.length > 0 && <button className="export-button" onClick={() => downloadCommentsCsv(visibleComments)}>⇩ Xuất CSV</button>}</div>
    {comments.length === 0 ? <div className="empty"><div>⌁</div><h3>Chưa có dữ liệu để phân tích</h3><p>Mở phiên Facebook, sau đó dán link bài viết để bắt đầu.</p></div> : <><div className="stats"><StatCard label="Tổng bình luận" value={summary.total} accent="blue" /><StatCard label="Bình luận gốc" value={summary.topLevel} accent="violet" /><StatCard label="Phản hồi" value={summary.replies} accent="amber" /><StatCard label="Dấu hiệu quan tâm" value={summary.prospects} accent="green" /></div><div className="toolbar"><div className="search">⌕<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo tên hoặc nội dung..." /></div><button className={onlyLeads ? 'filter active-filter' : 'filter'} onClick={() => setOnlyLeads((value) => !value)}>◉ Chỉ hiện khách tiềm năng</button><button className="filter" onClick={() => { setQuery(''); setOnlyLeads(false); }}>Đặt lại</button><span>{visibleComments.length} kết quả</span></div><div className="comment-list">{visibleComments.map((comment) => <CommentItem key={comment.id} comment={comment} isSaved={savedIds.has(comment.id)} onToggleSave={() => onToggleSave(comment)} />)}</div></>}
  </section>;
}
