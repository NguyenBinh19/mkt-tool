const INTENT_PATTERN = /giá|bao nhiêu|inbox|tư vấn|mua|đặt|quan tâm|liên hệ|sđt|phone|\bib\b|ship/i;

export function isPotentialLead(comment) {
  return INTENT_PATTERN.test(comment.message ?? '');
}

export function summarizeComments(comments) {
  const topLevel = comments.filter((comment) => !comment.parentId).length;
  return {
    total: comments.length,
    topLevel,
    replies: comments.length - topLevel,
    prospects: comments.filter(isPotentialLead).length
  };
}
