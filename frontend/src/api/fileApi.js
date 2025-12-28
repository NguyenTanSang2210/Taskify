import api from "../api";

export const fileApi = {
    upload: async (file) => {
        const formData = new FormData();
        formData.append("file", file);
        const res = await api.post("/upload", formData, {
            headers: {
                "Content-Type": "multipart/form-data",
            },
        });
        return res.data; // { fileName, fileUrl }
    }
};
