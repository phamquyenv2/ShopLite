import React, { useRef, useState } from 'react';
import {
    IonPage,
    IonContent,
    IonIcon,
    IonSpinner,
    IonToast,
    IonModal,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import { useParams, useLocation } from 'react-router-dom';
import {
    closeOutline,
    cameraOutline,
    calendarOutline,
    chevronDownOutline,
    searchOutline,
    person,
} from 'ionicons/icons';
import { customerService } from '../services/customer.service';
import { VIETNAM_PROVINCES, VIETNAM_BANKS } from '../utils/data-constants';
import './CustomerFormPage.css';

const GENDER_OPTS = [
    { value: '', label: 'Giới tính' },
    { value: 'male', label: 'Nam' },
    { value: 'female', label: 'Nữ' },
    { value: 'other', label: 'Khác' },
];

const CustomerFormPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const location = useLocation();
    const { id } = useParams<{ id?: string }>();

    const isEdit = location.pathname.includes('/edit');

    const [name, setName] = useState('');
    const [phone, setPhone] = useState('');
    const [phone2, setPhone2] = useState('');
    const [birthday, setBirthday] = useState('');
    const [gender, setGender] = useState('');
    const [code, setCode] = useState('');

    const [area, setArea] = useState('');
    const [address, setAddress] = useState('');

    const [email, setEmail] = useState('');
    const [facebook, setFacebook] = useState('');

    const [customerGroup, setCustomerGroup] = useState('');

    const [invoiceCustomerType, setInvoiceCustomerType] = useState('Cá nhân');
    const [invoiceBuyerName, setInvoiceBuyerName] = useState('');
    const [invoiceTaxCode, setInvoiceTaxCode] = useState('');
    const [invoiceAddress, setInvoiceAddress] = useState('');
    const [invoiceCity, setInvoiceCity] = useState('');
    const [invoiceIdCard, setInvoiceIdCard] = useState('');
    const [invoicePassport, setInvoicePassport] = useState('');
    const [invoiceEmail, setInvoiceEmail] = useState('');
    const [invoicePhone, setInvoicePhone] = useState('');
    const [invoiceBank, setInvoiceBank] = useState('');
    const [invoiceBankAccount, setInvoiceBankAccount] = useState('');

    const [note, setNote] = useState('');

    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);
    const [nameErr, setNameErr] = useState('');
    const [phoneErr, setPhoneErr] = useState('');

    const [activeModal, setActiveModal] = useState<'area' | 'invoiceCity' | 'invoiceBank' | null>(null);
    const [searchQuery, setSearchQuery] = useState('');

    const handleOpenModal = (type: 'area' | 'invoiceCity' | 'invoiceBank') => {
        setActiveModal(type);
        setSearchQuery('');
    };

    const handleSelectItem = (item: string) => {
        if (activeModal === 'area') setArea(item);
        else if (activeModal === 'invoiceCity') setInvoiceCity(item);
        else if (activeModal === 'invoiceBank') setInvoiceBank(item);
        setActiveModal(null);
    };

    const getModalData = () => {
        if (activeModal === 'area' || activeModal === 'invoiceCity') return VIETNAM_PROVINCES;
        if (activeModal === 'invoiceBank') return VIETNAM_BANKS;
        return [];
    };

    const filteredList = getModalData().filter(item => 
        item.toLowerCase().includes(searchQuery.toLowerCase())
    );
    const modalTitle = activeModal === 'area' ? 'Khu vực' : 
                       activeModal === 'invoiceCity' ? 'Tỉnh/Thành Phố' : 
                       activeModal === 'invoiceBank' ? 'Tên ngân hàng' : '';

    const dateRef = useRef<HTMLInputElement>(null);

    useIonViewWillEnter(() => {
        if (!isEdit) {
            setName(''); setPhone(''); setPhone2('');
            setBirthday(''); setGender(''); setCode('');
            setArea(''); setAddress(''); setEmail(''); setFacebook('');
            setCustomerGroup(''); setInvoiceCustomerType('Cá nhân');
            setInvoiceBuyerName(''); setInvoiceTaxCode(''); setInvoiceAddress('');
            setInvoiceCity(''); setInvoiceIdCard(''); setInvoicePassport('');
            setInvoiceEmail(''); setInvoicePhone(''); setInvoiceBank('');
            setInvoiceBankAccount(''); setNote('');
            setNameErr(''); setPhoneErr('');
            return;
        }
        if (!id) return;
        setLoading(true);
        customerService.getCustomerById(id)
            .then(c => {
                if (!c) return;
                setName(c.name ?? '');
                setPhone(c.phone ?? '');
                setCode(`KH${String(c.id).padStart(6, '0')}`);
                setNameErr(''); setPhoneErr('');
            })
            .catch(e => setToast(e?.message ?? 'Lỗi tải dữ liệu'))
            .finally(() => setLoading(false));
    });

    const validate = () => {
        let ok = true;
        if (!name.trim()) { setNameErr('Vui lòng nhập họ và tên'); ok = false; } else setNameErr('');
        if (!phone.trim()) { setPhoneErr('Vui lòng nhập số điện thoại'); ok = false; }
        else if (!/^(0|\+84)(3|5|7|8|9)\d{8}$/.test(phone.trim())) { setPhoneErr('SĐT không hợp lệ'); ok = false; }
        else setPhoneErr('');
        return ok;
    };

    const handleSave = async () => {
        if (!validate()) return;
        setSaving(true);
        try {
            if (isEdit && id) {
                await customerService.updateCustomer(id, { name: name.trim(), phone: phone.trim() });
                setToast('Cập nhật thành công');
            } else {
                await customerService.createCustomer({ name: name.trim(), phone: phone.trim() });
                setToast('Đã thêm khách hàng');
            }
            setTimeout(() => ionRouter.goBack(), 900);
        } catch (e: any) {
            setToast(e?.message ?? 'Không thể lưu');
        } finally {
            setSaving(false);
        }
    };

    const canSave = name.trim() !== '' && phone.trim() !== '' && !saving;

    return (
        <IonPage className="cf-page">
            {/* ─── HEADER ─────────────────────────────────────────────── */}
            <div className="cf-header">
                <button className="cf-btn-close" onClick={() => ionRouter.goBack()}>
                    <IonIcon icon={closeOutline} />
                </button>
                <span className="cf-header-title">
                    {isEdit ? 'Sửa khách hàng' : 'Thêm khách hàng'}
                </span>
                <button
                    className={`cf-btn-save${canSave ? ' active' : ''}`}
                    disabled={!canSave}
                    onClick={handleSave}
                >
                    {saving ? 'Đang lưu...' : 'Lưu'}
                </button>
            </div>

            {/* ─── CONTENT ────────────────────────────────────────────── */}
            <IonContent className="cf-content">
                {loading ? (
                    <div className="cf-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                ) : (
                    <div className="cf-body">

                        {/* AVATAR */}
                        <div className="cf-avatar-wrap">
                            <div className="cf-avatar-circle">
                                <IonIcon icon={person} className="cf-person-icon" />
                            </div>
                            <div className="cf-cam-badge">
                                <IonIcon icon={cameraOutline} className="cf-cam-icon" />
                            </div>
                        </div>

                        {/* Họ và tên */}
                        <div className={`cf-card${nameErr ? ' cf-card-err' : ''}`}>
                            <input
                                className="cf-inp"
                                type="text"
                                placeholder=""
                                value={name}
                                onChange={e => { setName(e.target.value); if (e.target.value.trim()) setNameErr(''); }}
                            />
                            {!name && (
                                <span className="cf-ph">
                                    Họ và tên <span className="cf-star">*</span>
                                </span>
                            )}
                            {nameErr && <p className="cf-err-msg">{nameErr}</p>}
                        </div>

                        {/* Số điện thoại */}
                        <div className={`cf-card${phoneErr ? ' cf-card-err' : ''}`}>
                            <input
                                className="cf-inp"
                                type="tel"
                                inputMode="tel"
                                placeholder="Số điện thoại"
                                value={phone}
                                onChange={e => { setPhone(e.target.value); if (e.target.value.trim()) setPhoneErr(''); }}
                            />
                            {phoneErr && <p className="cf-err-msg">{phoneErr}</p>}
                        </div>

                        {/* Số điện thoại 2 */}
                        <div className="cf-card">
                            <input
                                className="cf-inp"
                                type="tel"
                                inputMode="tel"
                                placeholder="Số điện thoại 2"
                                value={phone2}
                                onChange={e => setPhone2(e.target.value)}
                            />
                        </div>

                        {/* Ngày sinh & Giới tính */}
                        <div className="cf-row-cards">
                            {/* Ngày sinh */}
                            <div className="cf-card cf-flex-1">
                                <div className="cf-date-wrap">
                                    {!birthday && <span className="cf-ph cf-ph-abs">Ngày sinh</span>}
                                    <input
                                        ref={dateRef}
                                        type="date"
                                        className={`cf-inp cf-date-inp${birthday ? ' filled' : ''}`}
                                        value={birthday}
                                        onChange={e => setBirthday(e.target.value)}
                                    />
                                    <IonIcon icon={calendarOutline} className="cf-trail-icon" />
                                </div>
                            </div>
                            {/* Giới tính */}
                            <div className="cf-card cf-flex-1">
                                <div className="cf-sel-wrap">
                                    <select
                                        className={`cf-inp cf-sel${gender ? ' filled' : ''}`}
                                        value={gender}
                                        onChange={e => setGender(e.target.value)}
                                    >
                                        {GENDER_OPTS.map(o => (
                                            <option key={o.value} value={o.value}>{o.label}</option>
                                        ))}
                                    </select>
                                    <IonIcon icon={chevronDownOutline} className="cf-trail-icon" />
                                </div>
                            </div>
                        </div>

                        {/* Mã khách hàng */}
                        <div className="cf-card">
                            <input
                                className="cf-inp cf-inp-muted"
                                type="text"
                                placeholder="Mã khách hàng (Mã tự động)"
                                value={code}
                                readOnly={isEdit}
                                onChange={e => !isEdit && setCode(e.target.value)}
                            />
                        </div>

                        {/* SECTIONS */}
                        
                        {/* ĐỊA CHỈ */}
                        <div className="cf-sec-title">Địa chỉ</div>
                        <div className="cf-card">
                            <div className="cf-sel-wrap" onClick={() => handleOpenModal('area')}>
                                <div className={`cf-inp cf-sel${area ? ' filled' : ''}`}>
                                    {area || 'Khu vực'}
                                </div>
                                <IonIcon icon={chevronDownOutline} className="cf-trail-icon" />
                            </div>
                        </div>
                        <div className="cf-card">
                            <input
                                className="cf-inp"
                                type="text"
                                placeholder="Địa chỉ"
                                value={address}
                                onChange={e => setAddress(e.target.value)}
                            />
                        </div>

                        {/* LIÊN HỆ */}
                        <div className="cf-sec-title">Liên hệ</div>
                        <div className="cf-card">
                            <input
                                className="cf-inp"
                                type="email"
                                placeholder="Email"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                            />
                        </div>
                        <div className="cf-card">
                            <input
                                className="cf-inp"
                                type="text"
                                placeholder="Facebook"
                                value={facebook}
                                onChange={e => setFacebook(e.target.value)}
                            />
                        </div>

                        {/* NHÓM KHÁCH HÀNG */}
                        <div className="cf-card cf-mt-24">
                            <div className="cf-sel-wrap">
                                <select
                                    className={`cf-inp cf-sel${customerGroup ? ' filled' : ''}`}
                                    value={customerGroup}
                                    onChange={e => setCustomerGroup(e.target.value)}
                                >
                                    <option value="" disabled hidden>Nhóm khách hàng</option>
                                    <option value="VIP">VIP</option>
                                    <option value="Thường">Thường</option>
                                </select>
                                <IonIcon icon={chevronDownOutline} className="cf-trail-icon" />
                            </div>
                        </div>

                        {/* THÔNG TIN XUẤT HOÁ ĐƠN */}
                        <div className="cf-sec-title">Thông tin xuất hoá đơn</div>
                        <div className="cf-card">
                            <span className="cf-label-top">Loại khách hàng</span>
                            <div className="cf-sel-wrap">
                                <select
                                    className="cf-inp cf-sel filled"
                                    value={invoiceCustomerType}
                                    onChange={e => setInvoiceCustomerType(e.target.value)}
                                >
                                    <option value="Cá nhân">Cá nhân</option>
                                    <option value="Doanh nghiệp">Doanh nghiệp</option>
                                </select>
                                <IonIcon icon={chevronDownOutline} className="cf-trail-icon" />
                            </div>
                        </div>
                        <div className="cf-card">
                            <input
                                className="cf-inp"
                                type="text"
                                placeholder="Tên người mua"
                                value={invoiceBuyerName}
                                onChange={e => setInvoiceBuyerName(e.target.value)}
                            />
                        </div>
                        <div className="cf-card">
                            <div className="cf-input-with-action">
                                <input
                                    className="cf-inp"
                                    type="text"
                                    placeholder="Mã số thuế"
                                    value={invoiceTaxCode}
                                    onChange={e => setInvoiceTaxCode(e.target.value)}
                                />
                                <button className="cf-btn-action">Tra cứu</button>
                            </div>
                        </div>
                        <div className="cf-card">
                            <input
                                className="cf-inp"
                                type="text"
                                placeholder="Địa chỉ"
                                value={invoiceAddress}
                                onChange={e => setInvoiceAddress(e.target.value)}
                            />
                        </div>
                        <div className="cf-card">
                            <div className="cf-sel-wrap" onClick={() => handleOpenModal('invoiceCity')}>
                                <div className={`cf-inp cf-sel${invoiceCity ? ' filled' : ''}`}>
                                    {invoiceCity || 'Tỉnh/Thành Phố'}
                                </div>
                                <IonIcon icon={chevronDownOutline} className="cf-trail-icon" />
                            </div>
                        </div>
                        <div className="cf-card">
                            <input
                                className="cf-inp"
                                type="text"
                                placeholder="Số CMND/CCCD"
                                value={invoiceIdCard}
                                onChange={e => setInvoiceIdCard(e.target.value)}
                            />
                        </div>
                        <div className="cf-card">
                            <input
                                className="cf-inp"
                                type="text"
                                placeholder="Số hộ chiếu"
                                value={invoicePassport}
                                onChange={e => setInvoicePassport(e.target.value)}
                            />
                        </div>
                        <div className="cf-card">
                            <input
                                className="cf-inp"
                                type="email"
                                placeholder="Email"
                                value={invoiceEmail}
                                onChange={e => setInvoiceEmail(e.target.value)}
                            />
                        </div>
                        <div className="cf-card">
                            <input
                                className="cf-inp"
                                type="tel"
                                inputMode="tel"
                                placeholder="Số điện thoại"
                                value={invoicePhone}
                                onChange={e => setInvoicePhone(e.target.value)}
                            />
                        </div>
                        <div className="cf-card">
                            <div className="cf-sel-wrap" onClick={() => handleOpenModal('invoiceBank')}>
                                <div className={`cf-inp cf-sel${invoiceBank ? ' filled' : ''}`}>
                                    {invoiceBank || 'Tên ngân hàng'}
                                </div>
                                <IonIcon icon={chevronDownOutline} className="cf-trail-icon" />
                            </div>
                        </div>
                        <div className="cf-card">
                            <input
                                className="cf-inp"
                                type="text"
                                placeholder="Số tài khoản ngân hàng"
                                value={invoiceBankAccount}
                                onChange={e => setInvoiceBankAccount(e.target.value)}
                            />
                        </div>

                        {/* GHI CHÚ */}
                        <div className="cf-card cf-mt-24">
                            <span className="cf-label-top">Ghi chú</span>
                            <textarea
                                className="cf-inp cf-textarea"
                                placeholder="Nhập ghi chú"
                                value={note}
                                onChange={e => setNote(e.target.value)}
                                rows={4}
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

            <IonModal
                isOpen={activeModal !== null}
                onDidDismiss={() => setActiveModal(null)}
                initialBreakpoint={0.7}
                breakpoints={[0, 0.7, 0.95]}
                className="cf-select-modal"
            >
                <div className="cf-modal-content">
                    <div className="cf-modal-header">
                        <span className="cf-modal-title">{modalTitle}</span>
                        <button className="cf-modal-done" onClick={() => setActiveModal(null)}>Xong</button>
                    </div>
                    <div className="cf-modal-search-wrap">
                        <IonIcon icon={searchOutline} className="cf-search-icon" />
                        <input
                            className="cf-modal-search"
                            placeholder="Tìm kiếm"
                            value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                        />
                    </div>
                    <div className="cf-modal-list">
                        {filteredList.map(item => (
                            <div key={item} className="cf-modal-item" onClick={() => handleSelectItem(item)}>
                                {item}
                            </div>
                        ))}
                        {filteredList.length === 0 && (
                            <div className="cf-modal-empty">Không tìm thấy kết quả</div>
                        )}
                    </div>
                </div>
            </IonModal>
        </IonPage>
    );
};

export default CustomerFormPage;
