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
    arrowBackOutline,
    saveOutline,
    businessOutline,
    callOutline
} from 'ionicons/icons';
import { employeeService } from '../services/employee.service';
import { officeService } from '../services/office.service';
import { roleService } from '../services/role.service';
import { storeInvitationService } from '../services/storeInvitation.service';
import type { Employee, Office, Role } from '../api/types';
import { useStorePermissions } from '../utils/useStorePermissions';
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
    const { can } = useStorePermissions();
    const canInviteEmployee = can('/api/v1/store-invitations', 'POST');
    const canViewRoleDetail = can('/api/v1/roles/{id}', 'GET');
    const canUpdateEmployee = can('/api/v1/employees/{id}', 'PUT');
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [roles, setRoles] = useState<Role[]>([]);
    const [offices, setOffices] = useState<Office[]>([]);
    const [loading, setLoading] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [currentTab, setCurrentTab] = useState<'active' | 'role' | 'inactive'>('active');
    const [toast, setToast] = useState<string | null>(null);
    const [inviteOpen, setInviteOpen] = useState(false);
    const [inviteName, setInviteName] = useState('');
    const [invitePhone, setInvitePhone] = useState('');
    const [inviteRoleId, setInviteRoleId] = useState<string>('');
    const [inviteOfficeId, setInviteOfficeId] = useState<string>('');
    const [rolePickerOpen, setRolePickerOpen] = useState(false);
    const [officePickerOpen, setOfficePickerOpen] = useState(false);
    const [inviting, setInviting] = useState(false);
    const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null);
    const [salaryRate, setSalaryRate] = useState('');
    const [employeeNote, setEmployeeNote] = useState('');
    const [savingEmployee, setSavingEmployee] = useState(false);

    const loadData = async () => {
        setLoading(true);
        try {
            const [emps, rols, officeList] = await Promise.all([
                employeeService.getEmployees(),
                roleService.getRoles(),
                officeService.getOffices()
            ]);
            setEmployees(emps);
            setRoles(rols);
            setOffices(officeList);
            setInviteName('');
            setInvitePhone('');
            setInviteRoleId('');
            setInviteOfficeId(officeList[0]?.id ? String(officeList[0].id) : '');
            setRolePickerOpen(false);
            setOfficePickerOpen(false);
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
        if (!canInviteEmployee) {
            setToast('Bạn không có quyền thêm nhân viên');
            return;
        }
        const phone = invitePhone.trim();
        if (!phone) {
            setToast('Vui lòng nhâp số điện thoại');
            return;
        }
        if (!inviteRoleId) {
            setToast('Vui lòng chọn vai trò');
            return;
        }
        if (!inviteOfficeId) {
            setToast('Vui lòng chọn văn phòng');
            return;
        }
        setInviting(true);
        try {
            await storeInvitationService.createInvitation(phone, inviteRoleId, inviteOfficeId);
            setToast('Đã gửi lời mời');
            setInvitePhone('');
            setInviteOfficeId(offices[0]?.id ? String(offices[0].id) : '');
            setOfficePickerOpen(false);
            setInviteOpen(false);
        } catch (err: any) {
            setToast(err.message || 'Không thể gửi lời mời');
        } finally {
            setInviting(false);
        }
    };

    const openEmployeeDetail = (emp: Employee) => {
        ionRouter.push(`/employees/${emp.id}`);
    };

    const saveEmployeeSalary = async () => {
        if (!selectedEmployee || !canUpdateEmployee) return;
        const nextSalary = Number(salaryRate);
        if (!Number.isFinite(nextSalary) || nextSalary < 0) {
            setToast('Luong phai lon hon hoac bang 0');
            return;
        }
        if (!selectedEmployee.officeId) {
            setToast('Nhan vien chua co chi nhanh/van phong');
            return;
        }

        setSavingEmployee(true);
        try {
            const updated = await employeeService.updateEmployee(selectedEmployee.id, {
                userId: selectedEmployee.userId,
                officeId: selectedEmployee.officeId,
                salaryRate: nextSalary,
                qr: selectedEmployee.qr || null,
                note: employeeNote,
            });
            setEmployees(prev => prev.map(emp => emp.id === updated.id ? updated : emp));
            setSelectedEmployee(updated);
            setToast('Da cap nhat luong nhan vien');
        } catch (err: any) {
            setToast(err.message || 'Khong the cap nhat luong');
        } finally {
            setSavingEmployee(false);
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
                                        <div key={emp.id} className="ep-card" onClick={() => openEmployeeDetail(emp)} role="button" tabIndex={0}>
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
                                            onClick={() => canViewRoleDetail && ionRouter.push(`/roles/${role.id}`)}
                                            style={{ cursor: canViewRoleDetail ? 'pointer' : 'default' }}
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

                {canInviteEmployee && (
                    <IonFab vertical="bottom" horizontal="end" slot="fixed" style={{ marginBottom: '20px', marginRight: '8px' }}>
                        <IonFabButton className="ep-fab ep-fab-btn" onClick={() => setInviteOpen(true)}>
                            <IonIcon icon={addOutline} style={{ fontSize: '28px' }} />
                        </IonFabButton>
                    </IonFab>
                )}
            </IonContent>

            <IonModal isOpen={inviteOpen} onDidDismiss={() => {
                setInviteOpen(false);
                setRolePickerOpen(false);
                setOfficePickerOpen(false);
            }} className="ep-invite-modal">
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
                            <div className="ep-office-picker">
                                <button
                                    type="button"
                                    className={`ep-office-trigger ${inviteRoleId ? 'has-value' : ''}`}
                                    onClick={() => {
                                        setRolePickerOpen(open => !open);
                                        setOfficePickerOpen(false);
                                    }}
                                >
                                    <span>
                                        {roles.find(role => String(role.id) === inviteRoleId)?.name || 'Chọn 1 vai trò cho nhân viên'}
                                    </span>
                                    <IonIcon icon={chevronDownOutline} className={rolePickerOpen ? 'open' : ''} />
                                </button>
                                {rolePickerOpen && (
                                    <div className="ep-office-menu">
                                        {roles.length === 0 ? (
                                            <div className="ep-office-empty">Chưa có vai trò</div>
                                        ) : roles.map(role => {
                                            const selected = String(role.id) === inviteRoleId;
                                            return (
                                                <button
                                                    type="button"
                                                    key={role.id}
                                                    className={`ep-office-option ${selected ? 'selected' : ''}`}
                                                    onClick={() => {
                                                        setInviteRoleId(String(role.id));
                                                        setRolePickerOpen(false);
                                                    }}
                                                >
                                                    {role.name}
                                                </button>
                                            );
                                        })}
                                    </div>
                                )}
                            </div>
                        </div>
                        <div className="ep-invite-field">
                            <label>Văn phòng <span className="ep-invite-required">*</span></label>
                            <div className="ep-office-picker">
                                <button
                                    type="button"
                                    className={`ep-office-trigger ${inviteOfficeId ? 'has-value' : ''}`}
                                    onClick={() => {
                                        setOfficePickerOpen(open => !open);
                                        setRolePickerOpen(false);
                                    }}
                                >
                                    <span>
                                        {offices.find(office => String(office.id) === inviteOfficeId)?.name || 'Chọn văn phòng cho nhân viên'}
                                    </span>
                                    <IonIcon icon={chevronDownOutline} className={officePickerOpen ? 'open' : ''} />
                                </button>
                                {officePickerOpen && (
                                    <div className="ep-office-menu">
                                        {offices.length === 0 ? (
                                            <div className="ep-office-empty">Chưa có văn phòng</div>
                                        ) : offices.map(office => {
                                            const selected = String(office.id) === inviteOfficeId;
                                            return (
                                                <button
                                                    type="button"
                                                    key={office.id}
                                                    className={`ep-office-option ${selected ? 'selected' : ''}`}
                                                    onClick={() => {
                                                        setInviteOfficeId(String(office.id));
                                                        setOfficePickerOpen(false);
                                                    }}
                                                >
                                                    {office.name}
                                                </button>
                                            );
                                        })}
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </IonContent>
                <div className="ep-invite-footer">
                    <button
                        className="ep-invite-submit"
                        disabled={!inviteName.trim() || !invitePhone.trim() || !inviteRoleId || !inviteOfficeId || inviting}
                        onClick={submitInvitation}
                    >
                        {inviting ? 'Đang gửi...' : 'Tạo và gửi lời mời'}
                    </button>
                </div>
            </IonModal>

            <IonModal isOpen={selectedEmployee !== null} onDidDismiss={() => setSelectedEmployee(null)} className="ep-invite-modal">
                <div className="ep-invite-header">
                    <button className="ep-invite-back" onClick={() => setSelectedEmployee(null)}>
                        <IonIcon icon={chevronBackOutline} />
                    </button>
                    <span className="ep-invite-title">Thông tin nhân viên</span>
                </div>
                <IonContent style={{ '--background': '#f4f6f9' }}>
                    {selectedEmployee && (
                        <div className="ep-detail">
                            <div className="ep-detail-card ep-detail-profile">
                                <div className="ep-detail-avatar">
                                    {getInitials(selectedEmployee.username || 'N')}
                                </div>
                                <div>
                                    <div className="ep-detail-name">{selectedEmployee.username || 'Nhan vien'}</div>
                                    <div className="ep-detail-role">{selectedEmployee.roleName || 'Nhan vien'}</div>
                                </div>
                            </div>

                            <div className="ep-detail-card">
                                <div className="ep-detail-row">
                                    <IonIcon icon={callOutline} />
                                    <div>
                                        <span>Số điện thoại</span>
                                        <strong>{selectedEmployee.phone || '--'}</strong>
                                    </div>
                                </div>
                                <div className="ep-detail-row">
                                    <IonIcon icon={businessOutline} />
                                    <div>
                                        <span>Chi nhánh / văn phòng</span>
                                        <strong>{selectedEmployee.officeName || '--'}</strong>
                                    </div>
                                </div>
                                <div className="ep-detail-row">
                                    <IonIcon icon={cashOutline} />
                                    <div>
                                        <span>Lương hiện tại</span>
                                        <strong>{new Intl.NumberFormat('vi-VN').format(selectedEmployee.salaryRate || 0)} đ/giờ</strong>
                                    </div>
                                </div>
                            </div>

                            <div className="ep-detail-card">
                                <div className="ep-invite-field">
                                    <label>Lương theo giờ</label>
                                    <input
                                        type="number"
                                        min={0}
                                        inputMode="decimal"
                                        value={salaryRate}
                                        disabled={!canUpdateEmployee}
                                        onChange={e => setSalaryRate(e.target.value)}
                                    />
                                </div>
                                <div className="ep-invite-field">
                                    <label>Ghi chú</label>
                                    <input
                                        type="text"
                                        value={employeeNote}
                                        disabled={!canUpdateEmployee}
                                        onChange={e => setEmployeeNote(e.target.value)}
                                    />
                                </div>
                            </div>
                        </div>
                    )}
                </IonContent>
                {canUpdateEmployee && (
                    <div className="ep-invite-footer">
                        <button className="ep-invite-submit" disabled={savingEmployee || !selectedEmployee} onClick={saveEmployeeSalary}>
                            <IonIcon icon={saveOutline} />
                            {savingEmployee ? 'Đang lưu...' : 'Lưu thiết lập lương'}
                        </button>
                    </div>
                )}
            </IonModal>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default EmployeesPage;
