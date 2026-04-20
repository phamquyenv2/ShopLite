import React, { useState, useMemo, useEffect } from 'react';
import {
    IonPage,
    IonHeader,
    IonToolbar,
    IonContent,
    IonIcon,
    IonButtons,
    IonButton,
    IonFab,
    IonFabButton,
    IonSpinner,
    IonToast,
    useIonRouter,
    useIonViewWillEnter
} from '@ionic/react';
import {
    searchOutline,
    addOutline,
    shieldCheckmarkOutline,
    documentTextOutline,
    cashOutline,
    archiveOutline,
    personOutline,
    arrowForwardOutline,
    chevronBackOutline
} from 'ionicons/icons';
import { employeeService } from '../services/employee.service';
import { roleService } from '../services/role.service';
import type { Employee, Role } from '../api/types';
import './EmployeesPage.css';

const avatarColors = [
    { bg: '#8b9dfe', color: '#111' }, // purple-blue
    { bg: '#b26a00', color: '#fff' }, // brown-orange
    { bg: '#baccf5', color: '#111' }, // light blue
    { bg: '#e0e5ed', color: '#444' }, // gray
    { bg: '#feca57', color: '#111' }, // yellow
    { bg: '#ff6b6b', color: '#fff' }  // red
];

const getAvatarStyle = (name: string) => {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    const index = Math.abs(hash) % avatarColors.length;
    return avatarColors[index];
};

const getInitials = (name: string) => {
    if (!name) return 'U';
    const words = name.trim().split(' ');
    if (words.length >= 2) {
        return words[words.length - 1][0].toUpperCase();
    }
    return name[0].toUpperCase();
};

const getRoleIconAndStyle = (roleName: string) => {
    const name = roleName?.toUpperCase() || '';
    if (name.includes('ORDER')) {
        return { icon: documentTextOutline, colorClass: 'icon-blue' };
    }
    if (name.includes('CASHIER')) {
        return { icon: cashOutline, colorClass: 'icon-green' };
    }
    if (name.includes('WAREHOUSE')) {
        return { icon: archiveOutline, colorClass: 'icon-orange' };
    }
    if (name.includes('MANAGER') || name.includes('ADMIN')) {
        return { icon: personOutline, colorClass: 'icon-purple' };
    }
    return { icon: documentTextOutline, colorClass: 'icon-gray' };
};

const getRoleDescs = (roleName: string, desc: string) => {
    if (desc) {
        // e.g. "Nhân viên ghi đơn - tạo đơn, xem sản phẩm"
        const parts = desc.split('-');
        if (parts.length > 1) {
            return parts[1].split(',').map(s => s.trim());
        }
        return [desc];
    }
    return [];
};

const getRoleTitle = (desc: string, name: string) => {
    if (desc) {
        const parts = desc.split('-');
        if (parts.length > 0) return parts[0].trim();
    }
    return name;
};

const EmployeesPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [roles, setRoles] = useState<Role[]>([]);
    const [loading, setLoading] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [currentTab, setCurrentTab] = useState<'active' | 'role' | 'inactive'>('active');
    const [toast, setToast] = useState<string | null>(null);

    // Load data
    const loadData = async () => {
        setLoading(true);
        try {
            const [emps, rols] = await Promise.all([
                employeeService.getEmployees(),
                roleService.getRoles()
            ]);
            setEmployees(emps);
            setRoles(rols);
        } catch (err: any) {
            setToast(err.message || 'Lỗi khi tải dữ liệu');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => {
        loadData();
    });

    const [debouncedSearch, setDebouncedSearch] = useState('');
    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedSearch(searchQuery);
        }, 500);
        return () => clearTimeout(timer);
    }, [searchQuery]);

    // Filter employees
    const filteredEmployees = useMemo(() => {
        const query = debouncedSearch.toLowerCase();

        return employees.filter(emp => {
            // Filter by search
            const nameMatch = emp.username?.toLowerCase().includes(query) || false;
            const phoneMatch = emp.phone?.includes(query) || false;
            if (!nameMatch && !phoneMatch && query !== '') return false;

            // Filter by tab
            if (currentTab === 'active') return !emp.deleted;
            if (currentTab === 'inactive') return emp.deleted;
            return true;
        });
    }, [employees, debouncedSearch, currentTab]);

    // Search roles
    const filteredRoles = useMemo(() => {
        const query = debouncedSearch.toLowerCase();
        return roles.filter(r =>
            (r.name && r.name.toLowerCase().includes(query)) ||
            (r.description && r.description.toLowerCase().includes(query))
        );
    }, [roles, debouncedSearch]);

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
                            placeholder={currentTab === 'role' ? "Tìm kiếm vai trò..." : "Tìm kiếm nhân viên..."}
                            value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                        />
                    </div>
                </div>

                <div className="ep-segments">
                    <button
                        className={`ep-segment-btn ${currentTab === 'active' ? 'active' : ''}`}
                        onClick={() => setCurrentTab('active')}
                    >
                        Đang làm việc
                    </button>
                    <button
                        className={`ep-segment-btn ${currentTab === 'role' ? 'active' : ''}`}
                        onClick={() => setCurrentTab('role')}
                    >
                        Vai trò
                    </button>
                    <button
                        className={`ep-segment-btn ${currentTab === 'inactive' ? 'active' : ''}`}
                        onClick={() => setCurrentTab('inactive')}
                    >
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
                                ) : (
                                    filteredEmployees.map(emp => {
                                        const name = emp.username || 'Unknown';
                                        const style = getAvatarStyle(name);
                                        const isManager = emp.roleName?.toLowerCase().includes('manager');
                                        const roleBadgeClass = isManager ? 'badge-blue' : 'badge-gray';
                                        const roleLabel = emp.roleName || 'Nhân viên';

                                        return (
                                            <div key={emp.id} className="ep-card">
                                                <div
                                                    className="ep-card-avatar"
                                                    style={{ background: style.bg, color: style.color }}
                                                >
                                                    {getInitials(name)}
                                                    <div className={`ep-avatar-status ${emp.deleted ? 'status-inactive' : 'status-active'}`}></div>
                                                </div>

                                                <div className="ep-card-info">
                                                    <div className="ep-card-name">{name}</div>
                                                    <div className="ep-card-phone">{emp.phone || 'Chưa cập nhật'}</div>
                                                </div>

                                                <div className={`ep-card-badge ${roleBadgeClass}`}>
                                                    {roleLabel}
                                                </div>
                                            </div>
                                        );
                                    })
                                )}
                            </div>
                        )}

                        {currentTab === 'role' && (
                            <div className="ep-list role-list">
                                <div className="role-banner">
                                    <div className="role-banner-content">
                                        <h3 className="role-banner-title">Phân quyền thông minh</h3>
                                        <p className="role-banner-desc">Tối ưu hóa quy trình làm việc bằng cách cấp đúng quyền cho đúng người. Bảo mật dữ liệu cửa hàng tuyệt đối.</p>
                                    </div>
                                    <IonIcon icon={shieldCheckmarkOutline} className="role-banner-icon" />
                                </div>

                                {filteredRoles.length === 0 ? (
                                    <div className="ep-empty">Không có vai trò nào</div>
                                ) : (
                                    filteredRoles.map(role => {
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
                                    })
                                )}
                            </div>
                        )}
                    </>
                )}

                <IonFab vertical="bottom" horizontal="end" slot="fixed" style={{ marginBottom: '20px', marginRight: '8px' }}>
                    <IonFabButton className="ep-fab ep-fab-btn" onClick={() => setToast('Tính năng đang phát triển')}>
                        <IonIcon icon={addOutline} style={{ fontSize: '28px' }} />
                    </IonFabButton>
                </IonFab>

            </IonContent>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default EmployeesPage;
