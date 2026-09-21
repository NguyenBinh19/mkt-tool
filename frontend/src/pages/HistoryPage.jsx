import { EmptyState } from './LeadsPage';

export function HistoryPage({ items, onClear }) {
  return <section className="page-panel"><div className="panel-head"><div><p className="eyebrow">WORKSPACE ACTIVITY</p><h2>Lịch sử trích xuất</h2><p className="panel-subtitle">Theo dõi các bài viết đã phân tích trong trình duyệt này.</p></div><button className="danger-button" disabled={!items.length} onClick={onClear}>Xoá lịch sử</button></div>{!items.length ? <EmptyState icon="◷" title="Chưa có phiên nào" text="Các lần extract thành công sẽ được ghi lại tại đây." /> : <div className="history-list">{items.map((item) => <article className="history-row" key={item.id}><div className="history-icon">↗</div><div className="history-main"><strong>{item.url}</strong><small>{new Date(item.createdAt).toLocaleString('vi-VN')}</small></div><div className="history-count"><b>{item.count}</b><span>comment</span></div><span className="history-status">Hoàn tất</span></article>)}</div>}</section>;
}
