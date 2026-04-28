

const CLOUD_NAME = 'dqbheiddg';
const UPLOAD_PRESET = 'shoplite_unsigned';
const UPLOAD_URL = `https://api.cloudinary.com/v1_1/${CLOUD_NAME}/auto/upload`;

export const uploadToCloudinary = async (file: File): Promise<string | null> => {
    if (!file) return null;

    const data = new FormData();
    data.append('file', file);
    data.append('upload_preset', UPLOAD_PRESET);
    data.append('cloud_name', CLOUD_NAME);

    try {
        const res = await fetch(UPLOAD_URL, {
            method: 'POST',
            body: data,
        });

        const result = await res.json();

        if (result.secure_url) {
            return result.secure_url as string;
        } else {
            console.error('Cloudinary upload error:', result);
            return null;
        }
    } catch (error) {
        console.error('Upload to Cloudinary failed:', error);
        return null;
    }
};
