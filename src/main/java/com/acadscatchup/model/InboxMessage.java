package com.acadscatchup.model;

/**
 * Model representing an inbox message or deficiency submission notification.
 * @author F4TAL (Stevenson James G. Gastanes)
 */
public class InboxMessage {

    public static final String DEVELOPER = "F4TAL";

    private int     id;
    private int     senderId;
    private String  senderName;
    private String  senderRole;
    private int     recipientId;
    private String  recipientName;
    private String  title;
    private String  message;
    private Integer itemId;
    private String  itemName;
    private String  subjectCode;
    private String  msgType;     // "SUBMISSION", "REPORT_RESOLVED", "SYSTEM"
    private String  attachmentType; // "LINK", "FILE"
    private String  attachmentName; // file name or link title
    private String  attachmentUrl;  // URL or Base64 file data
    private boolean isRead;
    private String  createdAt;

    public InboxMessage() {}

    public InboxMessage(int id, int senderId, String senderName, String senderRole,
                        int recipientId, String recipientName, String title, String message,
                        Integer itemId, String itemName, String subjectCode,
                        String msgType, boolean isRead, String createdAt) {
        this(id, senderId, senderName, senderRole, recipientId, recipientName, title, message,
             itemId, itemName, subjectCode, msgType, null, null, null, isRead, createdAt);
    }

    public InboxMessage(int id, int senderId, String senderName, String senderRole,
                        int recipientId, String recipientName, String title, String message,
                        Integer itemId, String itemName, String subjectCode,
                        String msgType, String attachmentType, String attachmentName, String attachmentUrl,
                        boolean isRead, String createdAt) {
        this.id             = id;
        this.senderId       = senderId;
        this.senderName     = senderName;
        this.senderRole     = senderRole;
        this.recipientId    = recipientId;
        this.recipientName  = recipientName;
        this.title          = title;
        this.message        = message;
        this.itemId         = itemId;
        this.itemName       = itemName;
        this.subjectCode    = subjectCode;
        this.msgType        = msgType;
        this.attachmentType = attachmentType;
        this.attachmentName = attachmentName;
        this.attachmentUrl  = attachmentUrl;
        this.isRead         = isRead;
        this.createdAt      = createdAt;
    }

    public int     getId()                    { return id; }
    public void    setId(int id)              { this.id = id; }

    public int     getSenderId()              { return senderId; }
    public void    setSenderId(int sId)       { this.senderId = sId; }

    public String  getSenderName()            { return senderName; }
    public void    setSenderName(String s)    { this.senderName = s; }

    public String  getSenderRole()            { return senderRole; }
    public void    setSenderRole(String r)    { this.senderRole = r; }

    public int     getRecipientId()           { return recipientId; }
    public void    setRecipientId(int rId)    { this.recipientId = rId; }

    public String  getRecipientName()         { return recipientName; }
    public void    setRecipientName(String r) { this.recipientName = r; }

    public String  getTitle()                 { return title; }
    public void    setTitle(String t)         { this.title = t; }

    public String  getMessage()               { return message; }
    public void    setMessage(String m)       { this.message = m; }

    public Integer getItemId()                { return itemId; }
    public void    setItemId(Integer i)       { this.itemId = i; }

    public String  getItemName()              { return itemName; }
    public void    setItemName(String in)     { this.itemName = in; }

    public String  getSubjectCode()           { return subjectCode; }
    public void    setSubjectCode(String sc)  { this.subjectCode = sc; }

    public String  getMsgType()               { return msgType; }
    public void    setMsgType(String mt)      { this.msgType = mt; }

    public boolean isRead()                   { return isRead; }
    public void    setRead(boolean read)      { isRead = read; }

    public String  getCreatedAt()             { return createdAt; }
    public void    setCreatedAt(String ca)    { this.createdAt = ca; }

    public String  getAttachmentType()         { return attachmentType; }
    public void    setAttachmentType(String t) { this.attachmentType = t; }

    public String  getAttachmentName()         { return attachmentName; }
    public void    setAttachmentName(String n) { this.attachmentName = n; }

    public String  getAttachmentUrl()          { return attachmentUrl; }
    public void    setAttachmentUrl(String u)  { this.attachmentUrl = u; }

    public boolean hasAttachment() {
        return attachmentUrl != null && !attachmentUrl.isBlank();
    }

    public String getTypeBadge() {
        if (msgType == null || msgType.isBlank()) return "Notice";
        String mt = msgType.trim();
        if (mt.equalsIgnoreCase("SUBMISSION")) return "Submission";
        if (mt.equalsIgnoreCase("GRADED")) return "Graded";
        if (mt.equalsIgnoreCase("REPORT_RESOLVED")) return "Bug Resolved";
        if (mt.equalsIgnoreCase("UPDATE") || mt.equalsIgnoreCase("SYSTEM_UPDATE")) return "Update";
        return mt;
    }
}
