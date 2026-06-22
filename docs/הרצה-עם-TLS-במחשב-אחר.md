# הרצת הפרויקט עם TLS על מחשב אחר

**פרויקט:** chess-bagrut-java  
**תרחיש:** שרת + לקוח על **אותו מחשב חדש** (localhost)  
**תאריך:** יוני 2026

---

## סקירה

מדריך זה מסביר איך להעביר את פרויקט השחמט למחשב אחר ולהריץ אותו **עם TLS מופעל**.

הנחות:
- שרת ולקוח רצים על **אותו מחשב**
- החיבור הוא ל-`127.0.0.1` (localhost)
- סיסמת ברירת מחדל לקבצי JKS: `changeit`

---

## דרישות מקדימות

| דרישה | איך לבדוק |
|--------|-----------|
| Windows | — |
| JDK 17 ומעלה | `java -version` |
| `keytool` (מגיע עם JDK) | `keytool` בטרמינל |

---

## שלב 1 — העתקת הפרויקט

העבר למחשב החדש את תיקיית הפרויקט, למשל:

```
C:\Users\<שם>\Documents\chess-bagrut-java\
```

### מה להעתיק

| להעתיק | הערה |
|--------|------|
| `src\` | קוד מקור — חובה |
| `run.bat`, `start-server.bat`, `start-client.bat` | הרצה |
| `scripts\` | יצירת תעודות TLS |
| `config\*.properties.example` | תבניות הגדרה |
| `pom.xml` | אופציונלי (Maven) |

### מה לא חובה להעתיק

| לא חובה | למה |
|---------|-----|
| `bin\`, `target\` | יידורו מחדש במחשב החדש |
| `config\*.jks` מהמחשב הישן | עדיף ליצור מחדש (שלב 3) |
| `users.json` | נוצר אוטומטית בהרשמה ראשונה |

### Gemini (אופציונלי)

אם רוצים **Play vs Gemini**, העתק גם:
- `config\gemini.key`

או הגדר במחשב החדש משתנה סביבה `GOOGLE_API_KEY` (רק בטרמינל השרת).

---

## שלב 2 — יצירת תעודות TLS

פתח **PowerShell** בתיקיית הפרויקט:

```powershell
cd C:\path\to\chess-bagrut-java
.\scripts\generate-tls-certs.ps1
```

### מה נוצר

| קובץ | תפקיד |
|------|--------|
| `config\server-keystore.jks` | מפתח פרטי + תעודת שרת |
| `config\client-truststore.jks` | תעודה שהלקוח סומך עליה |
| `config\server-cert.cer` | ייצוא ביניים (לא חובה להרצה) |

**חשוב:** הרץ את הסקריפט **במחשב שבו תריץ** — לא להסתמך על תעודות מהמחשב הישן.

---

## שלב 3 — קבצי הגדרה

צור את קבצי ההגדרה מהדוגמאות:

```powershell
copy config\server.properties.example config\server.properties
copy config\client.properties.example config\client.properties
```

### `config\server.properties`

```properties
chess.tls.enabled=true
chess.tls.keystore=config/server-keystore.jks
chess.tls.keystore.password=changeit
chess.server.port=5555
chess.gemini.model=gemini-2.5-flash
chess.gemini.timeout.seconds=10
chess.gemini.fallback.enabled=true
```

### `config\client.properties`

```properties
chess.tls.enabled=true
chess.tls.truststore=config/client-truststore.jks
chess.tls.truststore.password=changeit
chess.server.host=127.0.0.1
chess.server.port=5555
```

### נקודות קריטיות

1. **שני הצדדים** חייבים `chess.tls.enabled=true`
2. סיסמאות JKS חייבות להתאים למה שנוצר בסקריפט (`changeit` כברירת מחדל)
3. `chess.server.host=127.0.0.1` — כי שרת ולקוח על אותו מחשב

---

## שלב 4 — הרצה

### טרמינל 1 — שרת

```powershell
cd C:\path\to\chess-bagrut-java
.\start-server.bat
```

**פלט צפוי:**
```text
Compiling...
OK.
Chess server started on port 5555 (TLS)
```

אם רואים `(TLS)` — הצפנה מופעלת.

### טרמינל 2 — לקוח

```powershell
cd C:\path\to\chess-bagrut-java
.\start-client.bat
```

1. Register או Login
2. Play vs Human / AI / Gemini

---

## שלב 5 — אימות ש-TLS עובד

| בדיקה | תוצאה תקינה |
|--------|-------------|
| לוג שרת | `Chess server started on port 5555 (TLS)` |
| Login בלקוח | נכנס ללובי בלי שגיאת חיבור |
| אין שגיאות בשרת | לא מופיע `SSLHandshakeException` |

### בדיקה שלילית (אופציונלי)

שנה בלקוח ל-`chess.tls.enabled=false` והשאר בשרת `true` — החיבור **אמור להיכשל**.  
מחזירים ל-`true` בשניהם.

---

## פתרון בעיות

| בעיה | סיבה אפשרית | פתרון |
|------|-------------|--------|
| `Cannot connect to server` | שרת לא רץ | הפעל `start-server.bat` קודם |
| `Cannot connect to server` | TLS לא מסונכרן | `tls.enabled=true` בשני הקבצים |
| `SSLHandshakeException` | אין truststore / לא תואם | הרץ שוב `generate-tls-certs.ps1` |
| אין `(TLS)` בלוג | `server.properties` חסר או `false` | צור קובץ עם `tls.enabled=true` |
| `keytool` לא מזוהה | JDK לא ב-PATH | התקן JDK 17+ |
| `Compilation failed` | אין JDK | התקן JDK, לא רק JRE |
| Gemini לא עובד | אין מפתח | העתק `config\gemini.key` או הגדר `GOOGLE_API_KEY` |

---

## הרצה מ-VS Code / Cursor

1. פתח את תיקיית הפרויקט בעורך
2. ודא ש-`config\server.properties` ו-`config\client.properties` קיימים עם TLS
3. **Run and Debug** → `Chess: Run Server`
4. **Run and Debug** → `Chess: Run Client`

הערה: העורך מקמפל ל-`target\classes` — אחרי שינוי קוד, בנה מחדש לפני debug.

---

## רשימת קבצים סופית (צ'קליסט)

```
chess-bagrut-java\
├── src\
├── run.bat
├── start-server.bat
├── start-client.bat
├── scripts\
│   └── generate-tls-certs.ps1
└── config\
    ├── server.properties          ← tls.enabled=true
    ├── client.properties          ← tls.enabled=true
    ├── server-keystore.jks        ← אחרי הסקריפט
    ├── client-truststore.jks        ← אחרי הסקריפט
    └── gemini.key                 ← אופציונלי
```

---

## תרחיש מורחב: שני מחשבים ברשת

אם בעתיד שרת על מחשב אחד ולקוח על מחשב שני:

1. במחשב השרת — הרץ `generate-tls-certs.ps1`
2. העתק `client-truststore.jks` למחשב הלקוח
3. בלקוח, ב-`client.properties`:
   ```properties
   chess.server.host=192.168.x.x
   ```
   (IP של מחשב השרת — מ-`ipconfig`)
4. פתח פורט **5555** ב-Firewall של השרת

---

## קישור למסמכים נוספים

- `docs/TLS-הסבר.md` — הסבר תיאורטי על TLS בפרויקט

---

*סוף המסמך*
