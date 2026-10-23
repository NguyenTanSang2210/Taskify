import { TOPIC_STATUS, WORKSPACE_STATUS, statusLabel } from '../../constants/status';

export const getWorkspaceStatusLabel = (status) => statusLabel(WORKSPACE_STATUS, status);
export const NEXT_WORKSPACE_STATUS = {
    DRAFT: 'OPEN_TOPIC',
    OPEN_TOPIC: 'OPEN_REGISTRATION',
    OPEN_REGISTRATION: 'LOCK_REGISTRATION',
    LOCK_REGISTRATION: 'IN_PROGRESS',
    IN_PROGRESS: 'CLOSED',
};
export const getTopicStatusLabel = (status) => statusLabel(TOPIC_STATUS, status);
