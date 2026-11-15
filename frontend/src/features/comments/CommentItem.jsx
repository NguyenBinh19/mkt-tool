import { isPotentialLead } from '../../lib/leadScoring';
import { useI18n } from '../../i18n/I18nContext';
import { useState } from 'react';

export function CommentItem({ comment, isSaved, onToggleSave }) {
  const isLead = isPotentialLead(comment);
  const { t, locale } = useI18n();
  const [copied, setCopied] = useState(false);
  const profileUrl = comment.authorUrl;

  async function copyProfile() {
    if (!profileUrl) return;
    try {
      await navigator.clipboard.writeText(profileUrl);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setCopied(false);
    }
  }

  return <article className="comment"><div className="comment-avatar">{(comment.author || '?').slice(0, 1).toUpperCase()}</div><div className="comment-body"><div className="comment-meta"><strong>{comment.author || t('comments.unknown')}</strong>{comment.parentId && <span className="reply-badge">{t('comments.replies')}</span>}{isLead && <span className="lead-badge">{t('comments.signals')}</span>}</div><p>{comment.message || <em>{t('comments.noText')}</em>}</p><small>{comment.createdAt ? new Date(comment.createdAt).toLocaleString(locale) : t('comments.noTime')}</small>{comment.postUrl && <div className="comment-source"><a href={comment.postUrl} target="_blank" rel="noreferrer">{t('comments.openPost')}</a></div>}{profileUrl && <div className="profile-actions"><a href={profileUrl} target="_blank" rel="noreferrer">{t('comments.openProfile')}</a><button type="button" onClick={copyProfile}>{copied ? t('common.copied') : t('common.copyLink')}</button></div>}</div><button type="button" className={isSaved ? 'save saved' : 'save'} onClick={onToggleSave} aria-label={isSaved ? t('comments.removeLead') : t('comments.saveLead')}>{isSaved ? '★' : '☆'}</button></article>;
}
