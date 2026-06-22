# הצפנת TLS בפרויקט שחמט — מסמך הסבר

**פרויקט:** chess-bagrut-java  
**נושא:** איך TLS עובד בפרויקט  
**תאריך:** יוני 2026

---

## 1. מה זה TLS?

**TLS** (Transport Layer Security) הוא פרוטוקול שמצפין את התקשורת בין שני צדדים ברשת.

בפרויקט שלנו:
- **לקוח** — אפליקציית Swing (`ChessGUI`)
- **שרת** — `ChessServer` על פורט 5555

### בלי TLS
כל הנתונים עוברים בטקסט גלוי:
- שם משתמש וסיסמה ב-login
- מהלכי שחמט
- הודעות JSON

מי שמאזין לרשת (sniffing) יכול לקרוא הכל.

### עם TLS
הנתונים מוצפנים. גם אם מישהו יירט את התעבורה, יראה רק ג'יבריש בינארי.

**חשוב:** פרוטוקול המשחק לא משתנה — עדיין JSON, שורה אחת לכל הודעה. רק שכבת התעבורה מתחת מוצפנת.

---

## 2. איך TLS מיושם בפרויקט

### 2.1 קבצי הגדרה

| קובץ | צד | תפקיד |
|------|-----|--------|
| `config/server.properties` | שרת | הפעלת TLS, נתיב keystore |
| `config/client.properties` | לקוח | הפעלת TLS, נתיב truststore |

הדגל המרכזי:
```properties
chess.tls.enabled=true
```
ברירת מחדל: `false` (TCP רגיל).

### 2.2 השרת — `ChessServer.java`

```java
ServerSocket server = tls
    ? SslHelper.createServerSocket(port, config)
    : new ServerSocket(port);
```

- TLS כבוי → `ServerSocket` רגיל
- TLS דלוק → `SSLServerSocket` מוצפן

אחרי החיבור, `ClientHandler` עובד כרגיל — קורא/כותב JSON. ההצפנה שקופה לקוד העליון.

### 2.3 הלקוח — `NetworkClient.java`

```java
socket = config.isTlsEnabled()
    ? SslHelper.createClientSocket(host, port, config)
    : new Socket(host, port);
```

אותו עיקרון: `SSLSocket` במקום `Socket` רגיל.

### 2.4 הלב — `SslHelper.java`

**בשרת (`createServerSocket`):**
1. טוען keystore (קובץ JKS)
2. יוצר KeyManager — מזהה את השרת
3. פותח SSLServerSocket על הפורט

**בלקוח (`createClientSocket`):**
1. טוען truststore (קובץ JKS)
2. יוצר TrustManager — בודק שהשרת אמין
3. מתחבר עם SSLSocket

---

## 3. תעודות ומפתחות (JKS)

### מושגים

| מושג | הסבר |
|------|------|
| **מפתח פרטי** | סודי, נשאר אצל השרת |
| **מפתח ציבורי** | יכול להיות חשוף |
| **תעודה (Certificate)** | קובץ שמקשר בין זהות השרת למפתח הציבורי |
| **Keystore** | קובץ JKS אצל השרת — מפתח פרטי + תעודה |
| **Truststore** | קובץ JKS אצל הלקוח — רק תעודות לסמוך עליהן |

### הקבצים בפרויקט

| קובץ | מיקום | תוכן |
|------|--------|------|
| `server-keystore.jks` | שרת | מפתח פרטי + תעודת שרת |
| `client-truststore.jks` | לקוח | תעודת השרת בלבד |

נוצרים עם: `scripts/generate-tls-certs.ps1`

### למה שני קבצים?

```
server-keystore.jks          client-truststore.jks
┌─────────────────────┐    ┌─────────────────────┐
│ מפתח פרטי (סוד!)    │    │ תעודת השרת בלבד     │
│ + תעודת שרת         │ ─> │ (מפתח ציבורי)       │
└─────────────────────┘    └─────────────────────┘
     אצל השרת                    אצל הלקוח
```

הלקוח לא מקבל את המפתח הפרטי — רק את התעודה הציבורית, כדי לוודא שהוא מדבר עם השרת הנכון.

---

## 4. TLS Handshake — שלב אחר שלב

```
לקוח                                    שרת
  |                                       |
  |-------- "שלום, אני רוצה TLS" -------->|
  |                                       |
  |<------- תעודת השרת + מפתח ציבורי -----|
  |                                       |
  |  הלקוח בודק: האם אני סומך על         |
  |  התעודה? (truststore)                |
  |                                       |
  |-------- מפתח סשן מוצפן ------------->|
  |<------- אישור -----------------------|
  |                                       |
  |========= ערוץ מוצפן פעיל =============|
  |   {"type":"login","password":"..."}   |
```

### שלב 1 — Client Hello
הלקוח מבקש TLS ושולח גרסאות נתמכות.

### שלב 2 — Server Hello + Certificate
השרת שולח את התעודה מה-keystore.

### שלב 3 — אימות תעודה
הלקוח בודק:
1. התעודה לא פגה
2. השם (`CN=localhost`) תואם לשרת
3. התעודה חתומה על ידי CA מהימן

**בפרויקט (self-signed):** אין CA חיצוני — מייבאים ידנית את התעודה ל-truststore.

### שלב 4 — מפתח סשן
הצדדים מסכימים על מפתח סימטרי זמני (AES) לשיחה.

### שלב 5 — תעבורה מוצפנת
JSON רגיל מעל ערוץ מוצפן. Java מצפין/מפענח אוטומטית.

---

## 5. Self-signed לעומת CA אמיתי

| | Self-signed (הפרויקט) | CA אמיתי (אינטרנט) |
|---|----------------------|-------------------|
| מי חותם | אנחנו (`keytool`) | Let's Encrypt וכו' |
| אמון אוטומטי | לא — צריך truststore | כן |
| מתאים ל | localhost, בגרות | אתרים באינטרנט |

---

## 6. מה קורה אם משהו לא מסונכרן?

| מצב | תוצאה |
|-----|--------|
| שרת TLS, לקוח רגיל | חיבור נכשל |
| לקוח TLS, שרת רגיל | חיבור נכשל |
| TLS בשניהם, אין truststore | `SSLHandshakeException` |
| TLS בשניהם, הכל מוגדר | עובד — `(TLS)` בלוג השרת |

---

## 7. מה TLS עושה ומה לא

### TLS עושה
- מצפין תעבורת רשת
- מגן על סיסמה בדרך
- מגן על מהלכים והודעות JSON

### TLS לא עושה
- לא מחליף login (username/password)
- לא מצפין `users.json` על הדיסק
- לא קשור ל-Gemini API (זה HTTPS נפרד לגוגל)

**אימות משתמש** — `UserAuth` (SHA-256 + salt), נפרד מ-TLS.  
**הצפנת תעבורה** — TLS.

---

## 8. איך להפעיל TLS

1. הרץ `scripts/generate-tls-certs.ps1`
2. העתק `config/server.properties.example` → `config/server.properties`
3. העתק `config/client.properties.example` → `config/client.properties`
4. בשניהם: `chess.tls.enabled=true`
5. הפעל שרת, אחר כך לקוח

---

## 9. ארכיטקטורה — סיכום

```
┌─────────────┐         TLS מוצפן          ┌─────────────┐
│   לקוח      │ ◄──────────────────────► │   שרת       │
│  ChessGUI   │   JSON over SSLSocket    │ ChessServer │
│             │   פורט 5555              │             │
│ truststore  │                          │  keystore   │
└─────────────┘                          └─────────────┘
```

**בשורה אחת:** אותו JSON על אותו פורט, בתוך ערוץ מוצפן — מופעל/כבוי ב-`chess.tls.enabled`.

---

## 10. רלוונטיות לבגרות

1. **בעיה:** סיסמאות ומהלכים בטקסט גלוי
2. **פתרון:** TLS עם `SSLServerSocket` / `SSLSocket`
3. **אימות שרת:** תעודה self-signed + truststore
4. **אימות משתמש:** `UserAuth` — נפרד מ-TLS
5. **טכנולוגיה:** Java 17, JDK בלבד, ללא ספריות חיצוניות

---

## 11. קבצי קוד רלוונטיים

| קובץ | תפקיד |
|------|--------|
| `src/chess/config/SslHelper.java` | יצירת SSL sockets |
| `src/chess/config/AppConfig.java` | קריאת הגדרות TLS |
| `src/chess/server/ChessServer.java` | הפעלת שרת עם/בלי TLS |
| `src/chess/client/NetworkClient.java` | חיבור לקוח עם/בלי TLS |
| `scripts/generate-tls-certs.ps1` | יצירת תעודות |
| `config/server.properties.example` | דוגמת הגדרות שרת |
| `config/client.properties.example` | דוגמת הגדרות לקוח |
