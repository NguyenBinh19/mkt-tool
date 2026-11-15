import { useEffect, useRef, useState } from 'react';
import { usePersistentState } from './usePersistentState';
import { facebookApi } from '../lib/apiClient';
import { validateFacebookPostUrl } from '../lib/validation';
import { workspaceKey } from '../lib/workspaceStorage';
import { useI18n } from '../i18n/I18nContext';

const INITIAL_FORM = { postUrl: '', maxComments: 100, includeReplies: true };

export function useFacebookExtraction(userId, workspaceId = 'default', token) {
  const { t } = useI18n();
  const [form, setForm] = usePersistentState(workspaceKey(userId, workspaceId, 'extraction-form'), INITIAL_FORM);
  const [result, setResult] = usePersistentState(workspaceKey(userId, workspaceId, 'extraction-result'), null);
  const [isExtracting, setIsExtracting] = useState(false);
  const [job, setJob] = useState(null);
  const [notice, setNotice] = useState(null);
  const workspaceRef = useRef(workspaceId);

  useEffect(() => {
    workspaceRef.current = workspaceId;
    setJob(null);
    setNotice(null);
    setIsExtracting(false);
  }, [workspaceId]);

  useEffect(() => {
    let active = true;
    facebookApi.latestExtractionJob(workspaceId, token).then((latest) => {
      if (active && latest) applySnapshot(latest);
    }).catch(() => {});
    return () => { active = false; };
  }, [workspaceId, token]);

  useEffect(() => {
    if (!job?.id || !['QUEUED', 'RUNNING'].includes(job.status)) return undefined;
    let active = true;
    const timer = window.setInterval(() => {
      facebookApi.extractionJob(job.id, token).then((latest) => {
        if (active && workspaceRef.current === workspaceId) applySnapshot(latest);
      }).catch((error) => {
        if (active) setNotice({ type: 'error', text: error.message });
      });
    }, 3000);
    return () => { active = false; window.clearInterval(timer); };
  }, [job?.id, job?.status, token, workspaceId]);

  function applySnapshot(snapshot) {
    setJob(snapshot);
    setIsExtracting(['QUEUED', 'RUNNING'].includes(snapshot.status));
    if (snapshot.result) setResult(snapshot.result);
    if (snapshot.status === 'COMPLETED') {
      setNotice({ type: 'success', text: t('extract.extracted', { count: snapshot.result?.comments?.length ?? 0 }) });
    } else if (snapshot.status === 'FAILED') {
      setNotice({ type: 'error', text: snapshot.result?.status === 'authentication_required' ? t('extract.authRequired') : snapshot.error || t('extract.failed') });
    }
  }

  const updateField = (field, value) => setForm((current) => ({ ...current, [field]: value }));

  async function extractComments() {
    const validationError = validateFacebookPostUrl(form.postUrl, t);
    if (validationError) {
      setNotice({ type: 'error', text: validationError });
      return null;
    }
    if (isExtracting) return null;
    const submittedUrl = form.postUrl;
    setIsExtracting(true);
    setJob({ status: 'QUEUED', postUrl: submittedUrl, createdAt: new Date().toISOString() });
    setNotice(null);
    try {
      const submitted = await facebookApi.submitExtractionJob({ ...form, workspaceId, maxComments: Number(form.maxComments) }, token);
      if (workspaceRef.current === workspaceId) applySnapshot(submitted);
      return submitted;
    } catch (error) {
      if (workspaceRef.current !== workspaceId) return null;
      setJob({ status: 'FAILED', postUrl: submittedUrl, finishedAt: new Date().toISOString() });
      setNotice({ type: 'error', text: error.message });
      setIsExtracting(false);
      return null;
    }
  }

  async function clearResult() {
    try {
      await facebookApi.clearExtractionJobs(workspaceId, token);
      setResult(null);
      if (!['QUEUED', 'RUNNING'].includes(job?.status)) setJob(null);
      setNotice(null);
    } catch (error) {
      setNotice({ type: 'error', text: error.message });
    }
  }

  return { form, result, isExtracting, job, notice, updateField, extractComments, clearResult };
}
