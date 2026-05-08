import React, { useState } from 'react';
import {
    IonPage,
    IonHeader,
    IonToolbar,
    IonContent,
    IonIcon,
    IonButtons,
    IonButton,
    IonSpinner,
    IonToast,
    useIonRouter,
    useIonViewWillEnter,
    IonFooter
} from '@ionic/react';
import { arrowBackOutline, closeCircle, ellipsisVertical, informationCircle, chevronDownOutline, chevronUpOutline } from 'ionicons/icons';
import { useParams } from 'react-router';
import { roleService } from '../../services/role.service';
import { permissionService } from '../../services/permission.service';
import type { Role, Permission } from '../../api/types';
import { requestStorePermissionsRefresh, useStorePermissions } from '../../utils/useStorePermissions';
import './RoleDetailPage.css';

const RoleDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const ionRouter = useIonRouter();
    const { can } = useStorePermissions();
    const canUpdateRole = can('/api/v1/roles/{id}', 'PUT');
    const canDeleteRole = can('/api/v1/roles/{id}', 'DELETE');
    
    const [role, setRole] = useState<Role | null>(null);
    const [allPermissions, setAllPermissions] = useState<Permission[]>([]);
    
    // Form state
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [selectedPermIds, setSelectedPermIds] = useState<Set<number>>(new Set());
    const [expandedModules, setExpandedModules] = useState<Set<string>>(new Set());
    
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const loadData = async () => {
        setLoading(true);
        try {
            const [roleData, perms] = await Promise.all([
                roleService.getRoleById(id),
                permissionService.getPermissions()
            ]);
            
            setRole(roleData);
            setAllPermissions(perms);
            
            setName(roleData.name || '');
            setDescription(roleData.description || '');
            
            if (roleData.permissions) {
                const ids = new Set(roleData.permissions.map(p => p.id));
                setSelectedPermIds(ids);
            }
        } catch (err: any) {
            setToast(err.message || 'Lỗi khi tải dữ liệu vai trò');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => {
        loadData();
    });

    const togglePermission = (permId: number) => {
        if (!canUpdateRole) return;
        const next = new Set(selectedPermIds);
        if (next.has(permId)) {
            next.delete(permId);
        } else {
            next.add(permId);
        }
        setSelectedPermIds(next);
    };

    const toggleExpand = (mod: string) => {
        const next = new Set(expandedModules);
        if (next.has(mod)) {
            next.delete(mod);
        } else {
            next.add(mod);
        }
        setExpandedModules(next);
    };

    const handleSave = async () => {
        if (!name.trim()) {
            setToast('Vui lòng nhập tên vai trò');
            return;
        }

        setSaving(true);
        try {
            await roleService.updateRole(id, {
                name,
                description,
                permissionIds: Array.from(selectedPermIds),
                active: role?.active ?? true
            });
            requestStorePermissionsRefresh();
            setToast('Cập nhật vai trò thành công');
            setTimeout(() => {
                ionRouter.goBack();
            }, 500);
        } catch (err: any) {
            setToast(err.message || 'Lỗi khi lưu vai trò');
        } finally {
            setSaving(false);
        }
    };

    const groupedPermissions = React.useMemo(() => {
        const groups: Record<string, Permission[]> = {};
        allPermissions.forEach(p => {
            const mod = p.module || 'Khác';
            if (!groups[mod]) groups[mod] = [];
            groups[mod].push(p);
        });
        return groups;
    }, [allPermissions]);

    const translateModule = (mod: string) => {
        const map: Record<string, string> = {
            'AUTH': 'Hệ thống / Đăng nhập',
            'PRODUCTS': 'Quản lý Sản phẩm',
            'CATEGORIES': 'Quản lý Danh mục',
            'CUSTOMERS': 'Quản lý Khách hàng',
            'SUPPLIERS': 'Quản lý Nhà cung cấp',
            'UNITS': 'Quản lý Đơn vị tính',
            'ORDERS': 'Quản lý Đơn hàng',
            'TRANSACTIONS': 'Quản lý Giao dịch',
            'PAYMENT': 'Thanh toán',
            'WEBHOOK': 'Webhook',
            'DEVICE_TOKENS': 'Thông báo / Thiết bị',
            'ATTENDANCE': 'Chấm công',
            'ROSTER': 'Lịch làm việc',
            'PAYROLLS': 'Bảng lương',
            'EMPLOYEES': 'Quản lý Nhân viên',
            'OFFICES': 'Quản lý Chi nhánh',
            'IMPORT_ORDERS': 'Nhập hàng',
            'INVENTORY': 'Quản lý Kho hàng',
            'USERS': 'Quản lý Tài khoản',
            'ROLES': 'Quản lý Vai trò',
            'PERMISSIONS': 'Quản lý Phân quyền',
        };
        return map[mod] || mod;
    };

    return (
        <IonPage className="role-detail-page">
            <IonHeader className="rd-header">
                <IonToolbar className="rd-toolbar">
                    <IonButtons slot="start">
                        <IonButton onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={arrowBackOutline} />
                        </IonButton>
                    </IonButtons>
                    <div className="rd-title">Chi tiết vai trò</div>
                    <IonButtons slot="end">
                        <IonButton>
                            <IonIcon icon={ellipsisVertical} />
                        </IonButton>
                    </IonButtons>
                </IonToolbar>
            </IonHeader>

            <IonContent className="rd-content">
                {loading ? (
                    <div className="rd-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                ) : (
                    <div className="rd-container">
                        <div className="rd-info-banner">
                            <IonIcon icon={informationCircle} className="rd-info-icon" />
                            <span>Mọi chỉnh sửa sẽ được áp dụng cho tất cả nhân viên có vai trò này</span>
                        </div>

                        <div className="rd-form-group">
                            <label>Tên vai trò <span className="text-red">*</span></label>
                            <div className="rd-input-wrap">
                                <input 
                                    type="text" 
                                    value={name} 
                                    onChange={e => setName(e.target.value)} 
                                    disabled={!canUpdateRole}
                                    placeholder="Nhập tên vai trò"
                                />
                                {name && (
                                    <IonIcon 
                                        icon={closeCircle} 
                                        className="rd-clear-icon" 
                                        onClick={() => canUpdateRole && setName('')}
                                    />
                                )}
                            </div>
                        </div>

                        <div className="rd-form-group">
                            <label>Mô tả</label>
                            <textarea 
                                value={description} 
                                onChange={e => setDescription(e.target.value)} 
                                disabled={!canUpdateRole}
                                placeholder="Nhập mô tả vai trò..."
                                rows={3}
                            />
                        </div>

                        <div className="rd-section-title">Phân quyền</div>

                        <div className="rd-permissions-list">
                            {Object.entries(groupedPermissions).map(([mod, perms]) => {
                                const hasAll = perms.every(p => selectedPermIds.has(p.id));
                                const hasSome = perms.some(p => selectedPermIds.has(p.id));
                                const isExpanded = expandedModules.has(mod);
                                
                                const toggleModule = (e: React.MouseEvent) => {
                                    e.stopPropagation();
                                    const next = new Set(selectedPermIds);
                                    if (hasAll) {
                                        perms.forEach(p => next.delete(p.id));
                                    } else {
                                        perms.forEach(p => next.add(p.id));
                                    }
                                    setSelectedPermIds(next);
                                };

                                return (
                                    <div key={mod} className="rd-module-group">
                                        {/* Module Header */}
                                        <div className="rd-module-header" onClick={() => toggleExpand(mod)}>
                                            <div className="rd-module-left">
                                                <div 
                                                    className={`rd-checkbox-custom ${hasAll ? 'checked' : hasSome ? 'partial' : ''}`}
                                                    onClick={canUpdateRole ? toggleModule : undefined}
                                                >
                                                    {hasAll && <span className="rd-check-icon">✓</span>}
                                                    {!hasAll && hasSome && <span className="rd-partial-icon">−</span>}
                                                </div>
                                                <span className="rd-module-name">{translateModule(mod)}</span>
                                            </div>
                                            <IonIcon 
                                                icon={isExpanded ? chevronUpOutline : chevronDownOutline} 
                                                className="rd-chevron"
                                            />
                                        </div>

                                        {/* Expanded Permission List */}
                                        {isExpanded && (
                                            <div className="rd-module-children">
                                                {perms.map(p => (
                                                    <div 
                                                        key={p.id} 
                                                        className="rd-perm-child"
                                                        onClick={() => togglePermission(p.id)}
                                                    >
                                                        <div className={`rd-checkbox-custom small ${selectedPermIds.has(p.id) ? 'checked' : ''}`}>
                                                            {selectedPermIds.has(p.id) && <span className="rd-check-icon">✓</span>}
                                                        </div>
                                                        <span className="rd-perm-name">{p.name}</span>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}
            </IonContent>

            {!loading && (canDeleteRole || canUpdateRole) && (
                <IonFooter className="rd-footer">
                    {canDeleteRole && (
                        <button className="rd-btn-delete" onClick={() => setToast('Tính năng xóa đang phát triển')}>Xóa</button>
                    )}
                    {canUpdateRole && (
                        <button className="rd-btn-save" onClick={handleSave} disabled={saving}>
                            {saving ? <IonSpinner name="dots" /> : 'Cập nhật'}
                        </button>
                    )}
                </IonFooter>
            )}

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default RoleDetailPage;
