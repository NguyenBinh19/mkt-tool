import { useState } from 'react';
import { Icon } from '../components/ui/Icon';
import { ToolCard } from '../features/tools/ToolCard';
import { toolCatalog } from '../features/tools/catalog';

const categories = ['Tất cả', ...new Set(toolCatalog.map((tool) => tool.category))];

export function ToolsPage({ user, onOpenTool, onRequestTool, canUse, requested, resultCount, leadCount, historyCount }) {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('Tất cả');
  const visible = toolCatalog.filter((tool) => (category === 'Tất cả' || tool.category === category) && (tool.label + ' ' + tool.text).toLocaleLowerCase().includes(query.toLocaleLowerCase()));

  return <section className="catalog-page">
    <div className="welcome-banner">
      <div className="welcome-copy"><p className="welcome-kicker">CHÀO {user.name.toLocaleUpperCase()} · SẴN SÀNG CHO CƠ HỘI MỚI?</p>
        <h2>Ý tưởng lớn.<br /><span>Công cụ đúng.</span></h2>
        <p>Một workspace cho hành trình marketing của bạn.<br />Từ khám phá hội thoại đến tìm thấy khách hàng tiếp theo.</p>
        <a className="welcome-cta" href="#tool-catalog">Khám phá công cụ <Icon name="arrow" size={18} /></a>
      </div>
      <div className="workflow-art" aria-hidden="true">
        <div className="art-orbit" /><div className="art-orbit inner" />
        <div className="art-center"><span>↗</span><small>leadflow</small></div>
        <div className="art-node art-source"><Icon name="chat" /><span>Hội thoại</span></div>
        <div className="art-node art-lead"><Icon name="target" /><span>Khách hàng</span></div>
        <div className="art-node art-growth"><Icon name="chart" /><span>Tăng trưởng</span></div>
        <span className="art-dot dot-one" /><span className="art-dot dot-two" />
      </div>
    </div>
    <div className="overview-stats">
      <div><Icon name="chat" /><span>Bình luận phiên này</span><strong>{resultCount}</strong></div>
      <div><Icon name="users" /><span>Lead đã lưu</span><strong>{leadCount}</strong></div>
      <div><Icon name="clock" /><span>Phiên trong lịch sử</span><strong>{historyCount}</strong></div>
    </div>
    <section id="tool-catalog" className="catalog-section">
      <div className="catalog-heading"><div><p className="eyebrow">BỘ CÔNG CỤ CỦA BẠN</p><h2>Làm nhiều hơn, trong một nơi.</h2></div><label className="catalog-search"><Icon name="search" size={18} /><input aria-label="Tìm công cụ" placeholder="Tìm công cụ…" value={query} onChange={(event) => setQuery(event.target.value)} /></label></div>
      <div className="catalog-filters" aria-label="Nhóm công cụ">{categories.map((item) => <button key={item} aria-pressed={category === item} onClick={() => setCategory(item)}>{item}</button>)}</div>
      <div className="catalog-grid">{visible.map((tool) => <ToolCard key={tool.id} tool={tool} allowed={canUse(tool.id)} pending={requested(tool.id)} onOpen={() => onOpenTool(tool.id)} onRequest={() => onRequestTool(tool.id)} />)}</div>
      {!visible.length && <div className="approval-empty"><span><Icon name="search" /></span><h3>Chưa tìm thấy công cụ phù hợp</h3><p>Thử một từ khóa khác hoặc chọn tất cả nhóm công cụ.</p><button className="ghost-button" onClick={() => { setQuery(''); setCategory('Tất cả'); }}>Đặt lại bộ lọc</button></div>}
    </section>
    <div className="workflow-note"><Icon name="target" /><div><strong>Một quy trình, nhiều cơ hội.</strong><p>Thu thập bình luận → nhận diện tín hiệu → lưu lead để chăm sóc.</p></div><span>01 — 02 — 03</span></div>
  </section>;
}
