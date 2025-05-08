# Copilot Pull Request Description Instructions

請根據以下規則生成 Pull Request 的標題和描述，遵循 **Conventional Commits 1.0.0** 規範，並確保清晰、專業且易於理解。

## General Response Language
- **IMPORTANT:** Please provide all responses, comments, and suggestions exclusively in **Traditional Chinese (繁體中文)**.

## 標題規則
1. **格式**: `<type>(<scope>): <description>`
   - `<type>`: 使用以下類型之一：
     - `feat`: 新功能
     - `fix`: 修復錯誤
     - `docs`: 文件變更
     - `style`: 程式碼風格調整（不影響功能）
     - `refactor`: 重構程式碼（不新增功能或修復錯誤）
     - `test`: 添加或修改測試
     - `chore`: 其他雜項（例如工具或配置更新）
   - `<scope>`: 可選，指定影響的模組或範圍（例如 `ui`、`api`、`auth`）。
   - `<description>`: 簡潔描述變更內容，使用祈使句（例如 "add user login"），限制在 50 個字符以內。
2. **範例**:
   - `feat(ui): add dark mode toggle`
   - `fix(api): resolve null pointer in user fetch`
   - `docs: update README with installation steps`

## 描述規則
1. **結構**:
   - 第一行：簡短總結變更（與標題一致）。
   - 空一行。
   - 正文：詳細說明變更的動機、內容或影響，使用 bullet points（`*`）列出。
   - 可選：加入元數據，例如關聯的 issue 號或重大變更（`BREAKING CHANGE`）。
2. **正文指南**:
   - 解釋 **為什麼** 進行此變更（動機或背景）。
   - 描述 **做了什麼**（具體改動）。
   - 提及 **影響範圍**（例如使用者、程式碼庫或測試）。
   - 每行限制在 72 個字符以內，保持可讀性。
3. **元數據**:
   - 使用 `Closes #<issue_number>` 關聯並關閉 issue。
   - 使用 `BREAKING CHANGE:` 標記重大變更並說明。

## 範例 Pull Request 描述
feat(auth): add OAuth2 login support
- 實作了 OAuth2 認證流程以支援第三方登入。
- 新增了 auth/oauth 模組處理 token 交換。
- 更新了 UserService 以支援新登入方式。
- 添加單元測試確保 token 驗證正確。
- 使用者現在可以透過 Google 登入應用程式。

Closes #123
BREAKING CHANGE: `login` API 現在需要額外的 `provider` 參數。

## 其他建議
- 保持語言簡潔、專業，避免冗長或模糊的描述。
- 如果有多個相關變更，逐項列出並說明。
- 若適用，參考項目規範或團隊慣例（例如特定關鍵字或格式）。
