const express = require('express');
const cors = require('cors'); // <--- 1. 引入 cors[cite: 2]
const multer = require('multer');
const path = require('path');

const app = express();
const PORT = 3000;

// <--- 2. 啟用 CORS，允許所有來源或指定來源呼叫[cite: 2]
app.use(cors());

const upload = multer({ dest: 'uploads/' });

app.use(express.static(path.join(__dirname, 'public')));
app.use(express.json());

// 接收遠端匯入 Git Log 的 API
app.post('/api/import-git-log', upload.single('gitLogFile'), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ success: false, message: '沒有收到上傳的檔案' });
  }

  console.log(`收到遠端上傳的 Git Log 檔案: ${req.file.originalname}`);

  res.json({
    success: true,
    message: `成功接收遠端 Git Log 檔案：${req.file.originalname}`,
    filename: req.file.originalname
  });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`伺服器運行中，監聽連接埠 ${PORT}`);
});