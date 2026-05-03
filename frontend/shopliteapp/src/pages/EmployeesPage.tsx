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
    IonModal,
    IonTitle,
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
    shieldCheckmarkOutline,
    chevronDownOutline,
    arrowBackOutline
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
    const [inviteName, setInviteName] = useState('');
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
            setInviteName('');
            setInvitePhone('');
            setInviteRoleId('');
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
            setToast('Vui lòng nhâp số điện thoại');
            return;
        }
        if (!inviteRoleId) {
            setToast('Vui lòng chọn vai trò');
            return;
        }
        setInviting(true);
        try {
            await storeInvitationService.createInvitation(phone, inviteRoleId);
            setToast('Đã gửi lời mời');
            setInvitePhone('');
            setInviteOpen(false);
        } catch (err: any) {
            setToast(err.message || 'Không thể gửi lời mời');
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
                    <div className="ep-title">Nhân viên</div>
                </IonToolbar>

                <div className="ep-search-container">
                    <div className="ep-search-bar">
                        <IonIcon icon={searchOutline} />
                        <input
                            type="text"
                            placeholder={currentTab === 'role' ? 'Tìm kiếm vai trò...' : 'Tìm kiếm nhân viên...'}
                            value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                        />
                    </div>
                </div>

                <div className="ep-segments">
                    <button className={`ep-segment-btn ${currentTab === 'active' ? 'active' : ''}`} onClick={() => setCurrentTab('active')}>
                        Đang làm việc
                    </button>
                    <button className={`ep-segment-btn ${currentTab === 'role' ? 'active' : ''}`} onClick={() => setCurrentTab('role')}>
                        Vai trò
                    </button>
                    <button className={`ep-segment-btn ${currentTab === 'inactive' ? 'active' : ''}`} onClick={() => setCurrentTab('inactive')}>
                        Đã nghỉ việc
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
                                    <div className="ep-empty">Không có nhân viên nào</div>
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
                                                <div className="ep-card-phone">{emp.phone || 'Chưa cập nhật'}</div>
                                            </div>

                                            <div className={`ep-card-badge ${roleBadgeClass}`}>
                                                {emp.roleName || 'Nhân viên'}
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
                                        <h3 className="role-banner-title">Phân quyền thông minh</h3>
                                        <p className="role-banner-desc">Cấp quyền cho từng vai trò để bảo vệ dữ liệu cửa hàng.</p>
                                    </div>
                                    <IonIcon icon={shieldCheckmarkOutline} className="role-banner-icon" />
                                </div>

                                {filteredRoles.length === 0 ? (
                                    <div className="ep-empty">Không có vai trò nào</div>
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

            <IonModal isOpen={inviteOpen} onDidDismiss={() => setInviteOpen(false)} className="ep-invite-modal">
                <div className="ep-invite-header">
                    <button className="ep-invite-back" onClick={() => setInviteOpen(false)}>
                        <IonIcon icon={chevronBackOutline} />
                    </button>
                    <span className="ep-invite-title">Thêm nhân viên</span>
                </div>
                <IonContent style={{ '--background': '#f4f6f9' }}>
                    <div className="ep-invite-form">
                        <div className="ep-invite-field">
                            <label>Tên nhân viên <span className="ep-invite-required">*</span></label>
                            <input
                                type="text"
                                placeholder="Ví dụ: Nguyễn Văn A"
                                value={inviteName}
                                onChange={e => setInviteName(e.target.value)}
                            />
                        </div>
                        <div className="ep-invite-field">
                            <label>Số điện thoại <span className="ep-invite-required">*</span></label>
                            <input
                                type="tel"
                                inputMode="tel"
                                placeholder="Ví dụ: 0912345678"
                                value={invitePhone}
                                onChange={e => setInvitePhone(e.target.value)}
                            />
                        </div>
                        <div className="ep-invite-field">
                            <label>Vai trò <span className="ep-invite-required">*</span></label>
                            <div className="ep-invite-select-wrap">
                                <select
                                    value={inviteRoleId}
                                    onChange={e => setInviteRoleId(e.target.value)}
                                >
                                    <option value="" disabled>Chọn 1 vai trò cho nhân viên</option>
                                    {roles.map(role => (
                                        <option key={role.id} value={role.id}>{role.name}</option>
                                    ))}
                                </select>
                                <IonIcon icon={chevronDownOutline} className="ep-invite-select-icon" />
                            </div>
                        </div>
                    </div>
                </IonContent>
                <div className="ep-invite-footer">
                    <button
                        className="ep-invite-submit"
                        disabled={!inviteName.trim() || !invitePhone.trim() || !inviteRoleId || inviting}
                        onClick={submitInvitation}
                    >
                        {inviting ? 'Đang gửi...' : 'Tạo và gửi lời mời'}
                    </button>
                </div>
            </IonModal>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default EmployeesPage;
