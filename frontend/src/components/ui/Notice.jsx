export function Notice({ notice }) {
  if (!notice) return null;
  return <div className={`notice ${notice.type}`}>{notice.type === 'success' ? '✓' : '!'} {notice.text}</div>;
}
