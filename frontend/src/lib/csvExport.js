function quote(value) {
  return `"${String(value ?? '').replaceAll('"', '""')}"`;
}

export function downloadCommentsCsv(comments) {
  const header = 'Tác giả,Nội dung,Loại,Thời gian';
  const rows = comments.map((comment) => [
    comment.author,
    comment.message,
    comment.parentId ? 'Phản hồi' : 'Bình luận',
    comment.createdAt
  ].map(quote).join(','));
  const file = new Blob([`\ufeff${[header, ...rows].join('\n')}`], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(file);
  const link = document.createElement('a');
  link.href = url;
  link.download = `facebook-comments-${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}
