import { useEffect, useState } from 'react';
import { CommentsPanel } from '../features/comments/CommentsPanel';
import { facebookApi } from '../lib/apiClient';
import { useI18n } from '../i18n/I18nContext';
import { EmptyState } from './LeadsPage';

export function ExtractionJobsPage({ jobs, hasMore, selectedId, workspaceId, token, savedIds, onToggleSave, onOpen, onBack }) {
  const { t, locale } = useI18n();
  const [extraJobs, setExtraJobs] = useState([]);
  const [nextPage, setNextPage] = useState(1);
  const [more, setMore] = useState(hasMore);
  const [loadingMore, setLoadingMore] = useState(false);
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setExtraJobs([]);
    setNextPage(1);
    setMore(hasMore);
  }, [workspaceId, jobs[0]?.id]);

  useEffect(() => {
    if (!extraJobs.length) setMore(hasMore);
  }, [hasMore, extraJobs.length]);

  useEffect(() => {
    if (!selectedId) {
      setDetail(null);
      return undefined;
    }
    let active = true;
    let timer;
    setDetail(null);
    setError('');
    const load = () => facebookApi.extractionJob(selectedId, token)
      .then((value) => {
        if (!active) return;
        setDetail(value);
        if (['COMPLETED', 'FAILED'].includes(value.status)) window.clearInterval(timer);
      })
      .catch((failure) => { if (active) setError(failure.message); });
    timer = window.setInterval(load, 3000);
    load();
    return () => { active = false; window.clearInterval(timer); };
  }, [selectedId, token]);

  async function loadMore() {
    setLoadingMore(true);
    setError('');
    try {
      const page = await facebookApi.extractionJobs(workspaceId, nextPage, token);
      setExtraJobs((current) => [...current, ...page.items]);
      setNextPage((current) => current + 1);
      setMore(page.hasMore);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setLoadingMore(false);
    }
  }

  if (selectedId) {
    const comments = detail?.result?.comments?.map((comment) => ({ ...comment, postUrl: detail.postUrl })) ?? [];
    return <section className="page-panel job-detail">
      <button type="button" className="history-back" onClick={onBack}>← {t('jobs.back')}</button>
      <div className="panel-head">
        <div>
          <p className="eyebrow">{t('jobs.eyebrow')}</p>
          <h2>{t('jobs.detailTitle')}</h2>
          <p className="panel-subtitle">{detail?.createdAt ? new Date(detail.createdAt).toLocaleString(locale) : t('jobs.loading')}</p>
        </div>
        {detail && <JobStatus status={detail.status} />}
      </div>
      {detail && <a className="history-post-link" href={detail.postUrl} target="_blank" rel="noreferrer">{detail.postUrl} ↗</a>}
      {!detail && !error && <p className="panel-subtitle">{t('jobs.loading')}</p>}
      {error && <p className="history-error" role="alert">{error}</p>}
      {detail?.status === 'FAILED' && <p className="history-error" role="alert">{detail.error || t('jobs.failed')}</p>}
      {['QUEUED', 'RUNNING'].includes(detail?.status) && <p className="panel-subtitle">{t(detail.status === 'QUEUED' ? 'jobs.waiting' : 'jobs.processing')}</p>}
      {detail?.status === 'COMPLETED' && (comments.length
        ? <CommentsPanel comments={comments} savedIds={savedIds} onToggleSave={onToggleSave} eyebrowKey="jobs.eyebrow" titleKey="jobs.commentsTitle" />
        : <EmptyState icon="⌁" title={t('jobs.noComments')} text={t('jobs.noCommentsText')} />)}
    </section>;
  }

  const firstPageIds = new Set(jobs.map((job) => job.id));
  const visibleJobs = [...jobs, ...extraJobs.filter((job) => {
    if (firstPageIds.has(job.id)) return false;
    firstPageIds.add(job.id);
    return true;
  })];
  return <section className="page-panel">
    <div className="panel-head">
      <div>
        <p className="eyebrow">{t('jobs.eyebrow')}</p>
        <h2>{t('jobs.title')}</h2>
        <p className="panel-subtitle">{t('jobs.description')}</p>
      </div>
    </div>
    {error && <p className="history-error" role="alert">{error}</p>}
    {!visibleJobs.length
      ? <EmptyState icon="◷" title={t('jobs.empty')} text={t('jobs.emptyText')} />
      : <div className="job-list">{visibleJobs.map((job) => <button type="button" className="job-row" key={job.id} onClick={() => onOpen(job.id)}>
        <JobStatus status={job.status} />
        <span className="job-main"><strong>{job.postUrl}</strong><small>{new Date(job.createdAt).toLocaleString(locale)}</small></span>
        <span className="job-open">{t('jobs.open')} →</span>
      </button>)}</div>}
    {more && <button type="button" className="ghost-button job-load-more" disabled={loadingMore} onClick={loadMore}>{loadingMore ? t('jobs.loading') : t('jobs.loadMore')}</button>}
  </section>;
}

function JobStatus({ status }) {
  const { t } = useI18n();
  return <span className={`job-status ${status.toLowerCase()}`}>{t(`jobs.status.${status.toLowerCase()}`)}</span>;
}
