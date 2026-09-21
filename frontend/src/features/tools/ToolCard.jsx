import { Icon } from '../../components/ui/Icon';

export function ToolCard({ tool, allowed, pending, onOpen, onRequest }) {
  const label = !tool.active ? 'Sắp ra mắt' : allowed ? 'Sẵn sàng sử dụng' : pending ? 'Chờ phê duyệt' : 'Cần cấp quyền';
  return <article className={`catalog-card ${tool.color}`}>
    <div className="catalog-card-top"><span className="catalog-icon"><Icon name={tool.icon} size={25} /></span><span className={`status-badge ${tool.active && allowed ? 'available' : ''}`}>{label}</span></div>
    <p className="catalog-category">{tool.category}</p><h3>{tool.label}</h3><p className="catalog-description">{tool.text}</p>
    <div className="catalog-card-bottom"><small>{tool.detail}</small>
      <button disabled={!tool.active || pending && !allowed} onClick={allowed ? onOpen : onRequest}>
        {!tool.active ? 'Đang phát triển' : allowed ? 'Mở công cụ' : pending ? 'Đã gửi yêu cầu' : 'Xin quyền truy cập'}<Icon name={allowed ? 'arrow' : 'shield'} size={16} />
      </button>
    </div>
  </article>;
}
