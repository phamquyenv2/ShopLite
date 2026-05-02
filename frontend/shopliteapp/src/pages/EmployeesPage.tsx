import React, { useEffect, useMemo, useState } from 'react';
import {
    IonButton,
    IonButtons,
    IonContent,
    IonFab,
    IonFabButton,
    IonHeader,
    IonIcon,
    IonPage,
    IonSpinner,
    IonToast,
    IonToolbar,
    useIonRouter,
    useIonViewWillEnter
} from '@ionic/react';
import {
    addOutline,
    archiveOutline,
    arrowForwardOutline,
    cashOutline,
    chevronBackOutline,
    documentTextOutline,
    personOutline,
    searchOutline,
    shieldCheckmarkOutline
} from 'ionicons/icons';
import { employeeService } from '../services/employee.service';
import { roleService } from '../services/role.service';
import { storeInvitationService } from '../services/storeInvitation.service';
import type { Employee, Role } from '../api/types';
import './EmployeesPage.css';

const avatarColors = [
    { bg: '#8b9dfe', color: '#111' },
    { bg: '#b26a00', color: '#fff' },
    { bg: '#baccf5', color: '#111' },
    { bg: '#e0e5ed', color: '#444' },
    { bg: '#feca57', color: '#111' },
    { bg: '#ff6b6b', color: '#fff' }
];

const getAvatarStyle = (name: string) => {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return avatarColors[Math.abs(hash) % avatarColors.length];
};

const getInitials = (name: string) => {
    if (!name) return 'U';
    const words = name.trim().split(' ');
    if (words.length >= 2) return words[words.length - 1][0].toUpperCase();
    return name[0].toUpperCase();
};

const getRoleIconAndStyle = (roleName: string) => {
    const name = roleName?.toUpperCase() || '';
    if (name.includes('ORDER')) return { icon: documentTextOutline, colorClass: 'icon-blue' };
    if (name.includes('CASHIER')) return { icon: cashOutline, colorClass: 'icon-green' };
    if (name.includes('WAREHOUSE')) return { icon: archiveOutline, colorClass: 'icon-orange' };
    if (name.includes('MANAGER') || name.includes('ADMIN')) return { icon: personOutline, colorClass: 'icon-purple' };
    return { icon: documentTextOutline, colorClass: 'icon-gray' };
};

const getRoleDescs = (_roleName: string, desc: string) => {
    if (!desc) return [];
    const parts = desc.split('-');
    if (parts.length > 1) return parts[1].split(',').map(s => s.trim());
    return [desc];
};

const getRoleTitle = (desc: string, name: string) => {
    if (!desc) return name;
    const parts = desc.split('-');
    return parts[0]?.trim() || name;
};

const EmployeesPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [roles, setRoles] = useState<Role[]>([]);
    const [loading, setLoading] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [currentTab, setCurrentTab] = useState<'active' | 'role' | 'inactive'>('active');
    const [toast, setToast] = useState<string | null>(null);
    const [inviteOpen, setInviteOpen] = useState(false);
    const [invitePhone, setInvitePhone] = useState('');
    const [inviteRoleId, setInviteRoleId] = useState<string>('');
    const [inviting, setInviting] = useState(false);

    const loadData = async () => {
        setLoading(true);
        try {
            const [emps, rols] = await Promise.all([
                employeeService.getEmployees(),
                roleService.getRoles()
            ]);
            setEmployees(emps);
            setRoles(rols);
            if (!inviteRoleId && rols.length > 0) setInviteRoleId(String(rols[0].id));
        } catch (err: any) {
            setToast(err.message || 'Loi khi tai du lieu');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => {
        loadData();
    });

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(searchQuery), 500);
        return () => clearTimeout(timer);
    }, [searchQuery]);

    const filteredEmployees = useMemo(() => {
        const query = debouncedSearch.toLowerCase();
        return employees.filter(emp => {
            const nameMatch = emp.username?.toLowerCase().includes(query) || false;
            const phoneMatch = emp.phone?.includes(query) || false;
            if (!nameMatch && !phoneMatch && query !== '') return false;
            if (currentTab === 'active') return !emp.deleted;
            if (currentTab === 'inactive') return emp.deleted;
            return true;
        });
    }, [employees, debouncedSearch, currentTab]);

    const filteredRoles = useMemo(() => {
        const query = debouncedSearch.toLowerCase();
        return roles.filter(r =>
            (r.name && r.name.toLowerCase().includes(query)) ||
            (r.description && r.description.toLowerCase().includes(query))
        );
    }, [roles, debouncedSearch]);

    const submitInvitation = async () => {
        const phone = invitePhone.trim();
        if (!phone) {
            setToast('Vui long nhap so dien thoai');
            return;
        }
        if (!inviteRoleId) {
            setToast('Vui long chon vai tro');
            return;
        }
        setInviting(true);
        try {
            await storeInvitationService.createInvitation(phone, inviteRoleId);
            setToast('Da gui loi moi');
            setInvitePhone('');
            setInviteOpen(false);
        } catch (err: any) {
            setToast(err.message || 'Khong the gui loi moi');
        } finally {
            setInviting(false);
        }
    };

    return (
        <IonPage className="employees-page">
            <IonHeader className="ep-header ion-no-border">
                <IonToolbar className="ep-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <div className="ep-title">Nhan vien</div>
                </IonToolbar>

                <div className="ep-search-container">
                    <div className="ep-search-bar">
                        <IonIcon icon={searchOutline} />
                        <input
                            type="text"
                            placeholder={currentTab === 'role' ? 'Tim kiem vai tro...' : 'Tim kiem nhan vien...'}
                            value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                        />
                    </div>
                </div>

                <div className="ep-segments">
                    <button className={`ep-segment-btn ${currentTab === 'active' ? 'active' : ''}`} onClick={() => setCurrentTab('active')}>
                        Dang lam viec
                    </button>
                    <button className={`ep-segment-btn ${currentTab === 'role' ? 'active' : ''}`} onClick={() => setCurrentTab('role')}>
                        Vai tro
                    </button>
                    <button className={`ep-segment-btn ${currentTab === 'inactive' ? 'active' : ''}`} onClick={() => setCurrentTab('inactive')}>
                        Da nghi viec
                    </button>
                </div>
            </IonHeader>

            <IonContent className="ep-content">
                {loading ? (
                    <div className="ep-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                ) : (
                    <>
                        {currentTab !== 'role' && (
                            <div className="ep-list">
                                {filteredEmployees.length === 0 ? (
                                    <div className="ep-empty">Khong co nhan vien nao</div>
                                ) : filteredEmployees.map(emp => {
                                    const name = emp.username || 'Unknown';
                                    const style = getAvatarStyle(name);
                                    const isManager = emp.roleName?.toLowerCase().includes('manager');
                                    const roleBadgeClass = isManager ? 'badge-blue' : 'badge-gray';

                                    return (
                                        <div key={emp.id} className="ep-card">
                                            <div className="ep-card-avatar" style={{ background: style.bg, color: style.color }}>
                                                {getInitials(name)}
                                                <div className={`ep-avatar-status ${emp.deleted ? 'status-inactive' : 'status-active'}`}></div>
                                            </div>

                                            <div className="ep-card-info">
                                                <div className="ep-card-name">{name}</div>
                                                <div className="ep-card-phone">{emp.phone || 'Chua cap nhat'}</div>
                                            </div>

                                            <div className={`ep-card-badge ${roleBadgeClass}`}>
                                                {emp.roleName || 'Nhan vien'}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}

                        {currentTab === 'role' && (
                            <div className="ep-list role-list">
                                <div className="role-banner">
                                    <div className="role-banner-content">
                                        <h3 className="role-banner-title">Phan quyen thong minh</h3>
                                        <p className="role-banner-desc">Cap dung quyen cho tung vai tro de bao ve du lieu cua hang.</p>
                                    </div>
                                    <IonIcon icon={shieldCheckmarkOutline} className="role-banner-icon" />
                                </div>

                                {filteredRoles.length === 0 ? (
                                    <div className="ep-empty">Khong co vai tro nao</div>
                                ) : filteredRoles.map(role => {
                                    const { icon, colorClass } = getRoleIconAndStyle(role.name);
                                    const title = getRoleTitle(role.description, role.name);
                                    const descs = getRoleDescs(role.name, role.description);

                                    return (
                                        <div
                                            key={role.id}
                                            className="role-card"
                                            onClick={() => ionRouter.push(`/roles/${role.id}`)}
                                            style={{ cursor: 'pointer' }}
                                        >
                                            <div className={`role-card-icon-wrap ${colorClass}`}>
                                                <IonIcon icon={icon} />
                                            </div>

                                            <div className="role-card-info">
                                                <div className="role-card-title">{title}</div>
                                                <div className="role-card-pills">
                                                    {descs.map((d, index) => (
                                                        <span key={index} className="role-pill">{d}</span>
                                                    ))}
                                                </div>
                                            </div>

                                            <IonIcon icon={arrowForwardOutline} className="role-card-arrow" />
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </>
                )}

                <IonFab vertical="bottom" horizontal="end" slot="fixed" style={{ marginBottom: '20px', marginRight: '8px' }}>
                    <IonFabButton className="ep-fab ep-fab-btn" onClick={() => setInviteOpen(true)}>
                        <IonIcon icon={addOutline} style={{ fontSize: '28px' }} />
                    </IonFabButton>
                </IonFab>
            </IonContent>

            {inviteOpen ? (
                <div className="invite-overlay">
                    <div className="invite-dialog">
                        <div className="invite-header">
                            <div>
                                <div className="invite-title">Moi member</div>
                                <div className="invite-subtitle">Nhap so dien thoai da dang ky va chon vai tro.</div>
                            </div>
                            <button type="button" className="invite-close" onClick={() => setInviteOpen(false)}>x</button>
                        </div>

                        <label className="invite-label" htmlFor="invite-phone">So dien thoai</label>
                        <input
                            id="invite-phone"
                            className="invite-input"
                            value={invitePhone}
                            onChange={e => setInvitePhone(e.target.value)}
                            placeholder="0901234567"
                            inputMode="tel"
                        />

                        <label className="invite-label" htmlFor="invite-role">Vai tro</label>
                        <select
                            id="invite-role"
                            className="invite-input"
                            value={inviteRoleId}
                            onChange={e => setInviteRoleId(e.target.value)}
                        >
                            {roles.map(role => (
                                <option key={role.id} value={role.id}>{role.name}</option>
                            ))}
                        </select>

                        <div className="invite-actions">
                            <button type="button" className="invite-secondary" onClick={() => setInviteOpen(false)}>
                                Huy
                            </button>
                            <button type="button" className="invite-primary" disabled={inviting} onClick={submitInvitation}>
                                {inviting ? 'Dang gui...' : 'Gui loi moi'}
                            </button>
                        </div>
                    </div>
                </div>
            ) : null}

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default EmployeesPage;
