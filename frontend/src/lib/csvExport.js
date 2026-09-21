function quote(value) {
  return `"${String(value ?? '').replaceAll('"', '""')}"`;
}

function downloadCsv(rows, header, filename) {
  const file = new Blob([`\ufeff${[header, ...rows].join('\n')}`], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(file);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

function commentRows(comments, t) {
  return comments.map((comment) => [
    comment.author,
    comment.authorUrl,
    comment.message,
    comment.parentId ? t('csv.reply') : t('csv.comment'),
    comment.createdAt
  ].map(quote).join(','));
}

export function downloadCommentsCsv(comments, t) {
  const header = [t('csv.author'), t('csv.profile'), t('csv.content'), t('csv.type'), t('csv.time')].map(quote).join(',');
  downloadCsv(commentRows(comments, t), header, `facebook-comments-${new Date().toISOString().slice(0, 10)}.csv`);
}

export function downloadLeadsCsv(leads, t) {
  const header = [t('csv.author'), t('csv.profile'), t('csv.content'), t('csv.type'), t('csv.time'), t('csv.savedAt')].map(quote).join(',');
  const rows = leads.map((lead) => [
    lead.author,
    lead.authorUrl,
    lead.message,
    lead.parentId ? t('csv.reply') : t('csv.comment'),
    lead.createdAt,
    lead.savedAt
  ].map(quote).join(','));
  downloadCsv(rows, header, `facebook-leads-${new Date().toISOString().slice(0, 10)}.csv`);
}
