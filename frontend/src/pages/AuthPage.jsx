import { useState } from 'react';

const copy = {
  login: { title: 'Mọi tín hiệu tăng trưởng, ở cùng một nơi.', subtitle: 'Đăng nhập vào marketing workspace giúp đội ngũ tìm kiếm, hiểu và chuyển đổi khách hàng tiềm năng nhanh hơn.' },
  register: { title: 'Bắt đầu vận hành marketing thông minh.', subtitle: 'Tạo tài khoản miễn phí. Sau khi admin duyệt, bạn sẽ có một workspace cho toàn bộ quy trình tìm kiếm khách hàng.' }
};

export function AuthPage({ users, onLogin, onRegister }) {
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [notice, setNotice] = useState(null);
  const content = copy[mode];

  function submit(event) {
    event.preventDefault();
    const email = form.email.trim().toLowerCase();
    if (!email || !form.password || (mode === 'register' && !form.name.trim())) return setNotice({ type: 'error', text: 'Vui lòng điền đầy đủ thông tin.' });
    if (mode === 'register') {
      if (users.some((user) => user.email === email)) return setNotice({ type: 'error', text: 'Email này đã được đăng ký.' });
      onRegister({ id: crypto.randomUUID(), name: form.name.trim(), email, password: form.password, role: 'member', status: 'pending', createdAt: new Date().toISOString() });
      setNotice({ type: 'success', text: 'Đã gửi yêu cầu. Admin sẽ xét duyệt tài khoản của bạn sớm.' });
      setMode('login');
      return;
    }
    const user = users.find((item) => item.email === email && item.password === form.password);
    if (!user) return setNotice({ type: 'error', text: 'Email hoặc mật khẩu chưa chính xác.' });
    if (user.status !== 'approved') return setNotice({ type: 'error', text: user.status === 'rejected' ? 'Yêu cầu của bạn chưa được chấp thuận.' : 'Tài khoản đang chờ admin xét duyệt.' });
    onLogin(user);
  }

  return <main className="auth-shell"><section className="auth-story"><a className="brand auth-brand" href="#top"><span className="brand-mark">↗</span><span>lead<span>flow</span></span></a><div className="auth-story-copy"><span className="pill">FACEBOOK INTELLIGENCE</span><h1>{content.title}</h1><p>{content.subtitle}</p><div className="auth-proof"><div><strong>10x</strong><span>ít thời gian lọc tay</span></div><div><strong>1 workspace</strong><span>cho cả team sales</span></div></div></div><p className="auth-footer">© 2026 Leadflow · Tăng trưởng có chủ đích</p></section><section className="auth-card-wrap"><div className="auth-card"><div className="auth-tabs"><button className={mode === 'login' ? 'active' : ''} onClick={() => { setMode('login'); setNotice(null); }}>Đăng nhập</button><button className={mode === 'register' ? 'active' : ''} onClick={() => { setMode('register'); setNotice(null); }}>Đăng ký</button></div><h2>{mode === 'login' ? 'Chào mừng trở lại' : 'Tạo tài khoản mới'}</h2><p className="auth-muted">{mode === 'login' ? 'Truy cập workspace của bạn.' : 'Mất chưa đến một phút để bắt đầu.'}</p><form onSubmit={submit}>{mode === 'register' && <label>Họ và tên<input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Nguyễn Minh Anh" /></label>}<label>Email công việc<input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="you@company.com" /></label><label>Mật khẩu<input type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder="••••••••" /></label><button className="primary-button auth-submit" type="submit">{mode === 'login' ? 'Vào workspace' : 'Gửi yêu cầu tham gia'} <span>→</span></button></form>{notice && <div className={`notice ${notice.type}`}>{notice.text}</div>}<p className="auth-note">Bằng việc tiếp tục, bạn đồng ý với điều khoản sử dụng và chính sách bảo mật của Leadflow.</p></div></section></main>;
}
