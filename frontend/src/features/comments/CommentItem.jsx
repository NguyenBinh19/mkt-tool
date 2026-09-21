import { isPotentialLead } from '../../lib/leadScoring';

export function CommentItem({ comment, isSaved, onToggleSave }) {
  const isLead = isPotentialLead(comment);
  return <article className="comment"><div className="comment-avatar">{(comment.author || '?').slice(0, 1).toUpperCase()}</div><div className="comment-body"><div className="comment-meta"><strong>{comment.author || 'Không rõ danh tính'}</strong>{comment.parentId && <span className="reply-badge">Phản hồi</span>}{isLead && <span className="lead-badge">Dấu hiệu quan tâm</span>}</div><p>{comment.message || <em>Bình luận không có nội dung văn bản (ảnh, sticker hoặc tệp đính kèm).</em>}</p><small>{comment.createdAt ? new Date(comment.createdAt).toLocaleString('vi-VN') : 'Thời gian không có sẵn'}</small></div><button className={isSaved ? 'save saved' : 'save'} onClick={onToggleSave} aria-label="Lưu khách hàng tiềm năng">{isSaved ? '★' : '☆'}</button></article>;
}
