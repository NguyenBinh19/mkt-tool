import { CommentItem } from '../features/comments/CommentItem';
import { isPotentialLead } from '../lib/leadScoring';
import { Icon } from '../components/ui/Icon';

export function LeadSignalsPage({ comments, savedIds, onToggleSave, onExtract, canExtract }) {
  const leads = comments.filter(isPotentialLead);
  return <section className="page-panel">
    <div className="panel-head"><div><p className="eyebrow">LEAD DISCOVERY</p><h2>Nhận diện người đang quan tâm</h2><p className="panel-subtitle">Lọc bình luận theo từ khóa như “giá”, “tư vấn”, “mua”. Đây là gợi ý để bạn xem xét, không phải dự đoán chuyển đổi.</p></div><span className="status-badge available">{leads.length} tín hiệu</span></div>
    {leads.length ? <div className="comment-list">{leads.map((comment) => <CommentItem key={comment.id} comment={comment} isSaved={savedIds.has(comment.id)} onToggleSave={() => onToggleSave(comment)} />)}</div>
      : <div className="approval-empty"><span><Icon name="target" size={30} /></span><h3>{comments.length ? 'Chưa tìm thấy tín hiệu phù hợp' : 'Bắt đầu từ một cuộc hội thoại'}</h3><p>{comments.length ? 'Bạn có thể xem toàn bộ bình luận và đánh dấu lead thủ công.' : 'Tín hiệu từ kết quả extract trong phiên này sẽ xuất hiện ở đây.'}</p>{canExtract && <button className="primary-button" onClick={onExtract}>Đến Extract comment <Icon name="arrow" size={16} /></button>}</div>}
  </section>;
}
