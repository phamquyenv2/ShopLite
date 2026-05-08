import React, { useState } from 'react';
import {
    IonPage,
    IonContent,
    IonIcon,
    IonSpinner,
    IonToast,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import { useParams, useLocation } from 'react-router-dom';
import { closeOutline } from 'ionicons/icons';
import { supplierService } from '../services/supplier.service';
import './SupplierFormPage.css';

const SupplierFormPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const location = useLocation();
    const { id } = useParams<{ id?: string }>();

    const isEdit = location.pathname.includes('/edit');

    const [name, setName] = useState('');
    const [phone, setPhone] = useState('');
    const [address, setAddress] = useState('');
    const [email, setEmail] = useState('');

    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);
    const [nameErr, setNameErr] = useState('');

    useIonViewWillEnter(() => {
        if (!isEdit) {
            setName(''); setPhone(''); setAddress(''); setEmail('');
            setNameErr('');
            return;
        }
        if (!id) return;
        setLoading(true);
        supplierService.getById(id)
            .then(s => {
                setName(s.name ?? '');
                setPhone(s.phone ?? '');
                setAddress(s.address ?? '');
                setEmail(s.email ?? '');
            })
            .catch(e => setToast(e?.message ?? 'Lỗi tải dữ liệu'))
            .finally(() => setLoading(false));
    });

    const validate = () => {
        if (!name.trim()) { setNameErr('Vui lòng nhập tên nhà cung cấp'); return false; }
        setNameErr('');
        return true;
    };

    const handleSave = async () => {
        if (!validate()) return;
        setSaving(true);
        try {
            const data = {
                name: name.trim(),
                phone: phone.trim() || undefined,
                address: address.trim() || undefined,
                email: email.trim() || undefined,
            };
            if (isEdit && id) {
                await supplierService.update(id, data);
                setToast('Cập nhật thành công');
            } else {
                await supplierService.create(data);
                setToast('Đã thêm nhà cung cấp');
            }
            setTimeout(() => ionRouter.goBack(), 900);
        } catch (e: any) {
            setToast(e?.message ?? 'Không thể lưu');
        } finally {
            setSaving(false);
        }
    };

    const canSave = name.trim() !== '' && !saving;

    return (
        <IonPage className="sf-page">
            {/* HEADER */}
            <div className="sf-header">
                <button className="sf-btn-close" onClick={() => ionRouter.goBack()}>
                    <IonIcon icon={closeOutline} />
                </button>
                <span className="sf-header-title">
                    {isEdit ? 'Sửa nhà cung cấp' : 'Thêm nhà cung cấp'}
                </span>
                <button
                    className={`sf-btn-save${canSave ? ' active' : ''}`}
                    disabled={!canSave}
                    onClick={handleSave}
                >
                    {saving ? 'Đang lưu...' : 'Lưu'}
                </button>
            </div>

            <IonContent className="sf-content">
                {loading ? (
                    <div className="sf-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                ) : (
                    <div className="sf-body">
                        {/* Tên nhà cung cấp */}
                        <div className={`sf-card${nameErr ? ' sf-card-err' : ''}`}>
                            <div className="sf-relative">
                                <input
                                    className="sf-inp"
                                    type="text"
                                    value={name}
                                    onChange={e => { setName(e.target.value); if (e.target.value.trim()) setNameErr(''); }}
                                />
                                {!name && (
                                    <span className="sf-ph">
                                        Tên nhà cung cấp <span className="sf-star">*</span>
                                    </span>
                                )}
                            </div>
                            {nameErr && <p className="sf-err-msg">{nameErr}</p>}
                        </div>

                        {/* Số điện thoại */}
                        <div className="sf-card">
                            <input
                                className="sf-inp"
                                type="tel"
                                inputMode="tel"
                                placeholder="Số điện thoại"
                                value={phone}
                                onChange={e => setPhone(e.target.value)}
                            />
                        </div>

                        {/* Địa chỉ */}
                        <div className="sf-card">
                            <input
                                className="sf-inp"
                                type="text"
                                placeholder="Địa chỉ"
                                value={address}
                                onChange={e => setAddress(e.target.value)}
                            />
                        </div>

                        {/* Email */}
                        <div className="sf-card">
                            <input
                                className="sf-inp"
                                type="email"
                                placeholder="Email"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                            />
                        </div>

                        <div style={{ height: 40 }} />
                    </div>
                )}
            </IonContent>

            <IonToast
                isOpen={toast !== null}
                message={toast ?? ''}
                duration={2200}
                onDidDismiss={() => setToast(null)}
            />
        </IonPage>
    );
};

export default SupplierFormPage;
