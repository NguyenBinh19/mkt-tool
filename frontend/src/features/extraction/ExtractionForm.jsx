import { Notice } from '../../components/ui/Notice';

export function ExtractionForm({ form, isLoading, notice, onChange, onSubmit }) {
  function submit(event) {
    event.preventDefault();
    onSubmit();
  }

  return <section className="extract-card"><div className="card-heading"><div><span className="step">01</span><h2>Trích xuất bài viết</h2><p>Kết nối với Facebook đã đăng nhập và lấy toàn bộ bình luận có thể xem được.</p></div><span className="secure">● Phiên cục bộ an toàn</span></div>
    <form onSubmit={submit}><label>Link bài viết Facebook<div className="url-field"><span>⌁</span><input value={form.postUrl} onChange={(event) => onChange('postUrl', event.target.value)} placeholder="Dán link bài viết Facebook tại đây..." required /></div></label>
      <div className="form-row"><label>Số lượng tối đa<input type="number" min="1" max="1000" value={form.maxComments} onChange={(event) => onChange('maxComments', event.target.value)} /></label><label className="toggle-row"><span>Thu thập cả phản hồi<small>Lấy comment con trong luồng thảo luận</small></span><input type="checkbox" checked={form.includeReplies} onChange={(event) => onChange('includeReplies', event.target.checked)} /><i /></label><button className="primary-button" disabled={isLoading}>{isLoading ? 'Đang xử lý…' : <>Bắt đầu trích xuất <span>→</span></>}</button></div>
    </form><Notice notice={notice} />
  </section>;
}
