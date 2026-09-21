import { useEffect, useRef, useState } from 'react';
import { usePersistentState } from './usePersistentState';
import { facebookApi } from '../lib/apiClient';
import { validateFacebookPostUrl } from '../lib/validation';
import { workspaceKey } from '../lib/workspaceStorage';
import { useI18n } from '../i18n/I18nContext';

const INITIAL_FORM = { postUrl: '', maxComments: 100, includeReplies: true };

export function useFacebookExtraction(userId, workspaceId = 'default') {
  const { t } = useI18n();
  const [form, setForm] = usePersistentState(workspaceKey(userId, workspaceId, 'extraction-form'), INITIAL_FORM);
  const [result, setResult] = usePersistentState(workspaceKey(userId, workspaceId, 'extraction-result'), null);
  const [isOpening, setIsOpening] = useState(false);
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

  const updateField = (field, value) => setForm((current) => ({ ...current, [field]: value }));

  async function openFacebookSession() {
    setIsOpening(true);
    setNotice(null);
    try {
      await facebookApi.openBrowser();
      setNotice({ type: 'success', text: t('extract.browserOpened') });
    } catch (error) {
      setNotice({ type: 'error', text: error.message });
    } finally {
      setIsOpening(false);
    }
  }

  async function extractComments() {
    const validationError = validateFacebookPostUrl(form.postUrl, t);
    if (validationError) {
      setNotice({ type: 'error', text: validationError });
      return null;
    }
    if (isExtracting) return null;
    const submittedUrl = form.postUrl;
    setIsExtracting(true);
    setJob({ status: 'running', url: submittedUrl, startedAt: new Date().toISOString() });
    setNotice(null);
    try {
      const data = await facebookApi.extractComments({ ...form, maxComments: Number(form.maxComments) });
      if (workspaceRef.current !== workspaceId) return data;
      setResult(data);
      setJob({ status: data.status === 'ok' ? 'completed' : 'failed', url: submittedUrl, finishedAt: new Date().toISOString(), count: data.comments?.length ?? 0 });
      const text = data.status === 'ok'
        ? t('extract.extracted', { count: data.comments?.length ?? 0 })
        : data.status === 'authentication_required' ? t('extract.authRequired') : t('extract.failed');
      setNotice({ type: data.status === 'ok' ? 'success' : 'error', text });
      return data;
    } catch (error) {
      if (workspaceRef.current !== workspaceId) return null;
      setJob({ status: 'failed', url: submittedUrl, finishedAt: new Date().toISOString() });
      setNotice({ type: 'error', text: error.message });
      return null;
    } finally {
      setIsExtracting(false);
    }
  }

  function clearResult() {
    setResult(null);
    setJob(null);
    setNotice(null);
  }

  return { form, result, isOpening, isExtracting, job, notice, updateField, openFacebookSession, extractComments, clearResult };
}
