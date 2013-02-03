package models;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.BooleanUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.DateTime;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;
import java.util.*;

/**
 * 
 */
@SuppressWarnings("serial")
@Entity
public class User extends Model {

    @Id
    public Long id;

    @Constraints.Required
    @Formats.NonEmpty
    @Column(unique = true)
    public String email;

    @Constraints.Required
    @Formats.NonEmpty
    public String fullname;

    @OneToOne(cascade = CascadeType.ALL)
    @JsonIgnore
    public ExternalUserId extUserId;
    
    @JsonIgnore
    public Boolean signUp;
    
    @Formats.NonEmpty
    @JsonIgnore
    public String tokenUuid;

    @Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonIgnore
    public DateTime tokenCreationTime;

    @Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonIgnore
    public DateTime tokenModificationTime;

    @Formats.NonEmpty
    @JsonIgnore
    public String passwordHash;

    @Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date dateCreation;

    @Formats.NonEmpty
    @JsonIgnore
    public Boolean validated = false;

    public Boolean admin = false;

    private Boolean notifOnMyTalk;

    private Boolean notifAdminOnAllTalk;

    private Boolean notifAdminOnTalkWithComment;

    @Constraints.Pattern("^([0-9a-fA-F][0-9a-fA-F]:){5}([0-9a-fA-F][0-9a-fA-F])$")
    public String adresseMac;

    @JsonProperty("provider")
    public String getProvider(){
        if (extUserId != null) {
            return extUserId.providerId;
        }
        return null;
    }

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<DynamicFieldValue> dynamicFieldValues;

    public List<DynamicFieldValue> getDynamicFieldValues() {
        if (dynamicFieldValues == null) {
            dynamicFieldValues = new ArrayList<DynamicFieldValue>();
        }
        return dynamicFieldValues;
    }

    @JsonProperty("dynamicFields")
    public List<DynamicFieldJson> getDynamicFieldsJson() {
        Map<Long, DynamicFieldValue> dynamicFieldValueByDynamicFieldId = new HashMap<Long, DynamicFieldValue>();
        for (DynamicFieldValue value : getDynamicFieldValues()) {
            dynamicFieldValueByDynamicFieldId.put(value.getDynamicField().getId(), value);
        }
        List<DynamicFieldJson> jsonFields = new ArrayList<DynamicFieldJson>();
        for (DynamicField field : DynamicField.find.all()) {
            jsonFields.add(DynamicFieldJson.toDynamicFieldJson(field, dynamicFieldValueByDynamicFieldId.get(field.getId())));
        }
        return jsonFields;
    }


    public boolean getNotifOnMyTalk() {
        return BooleanUtils.isNotFalse(notifOnMyTalk);
    }

    public boolean getNotifAdminOnAllTalk() {
        return BooleanUtils.isNotFalse(notifAdminOnAllTalk);
    }

    public boolean getNotifAdminOnTalkWithComment() {
        return BooleanUtils.isNotFalse(notifAdminOnTalkWithComment);
    }

    public void setNotifOnMyTalk(Boolean notifOnMyTalk) {
        this.notifOnMyTalk = notifOnMyTalk;
    }

    public void setNotifAdminOnAllTalk(Boolean notifAdminOnAllTalk) {
        this.notifAdminOnAllTalk = notifAdminOnAllTalk;
    }

    public void setNotifAdminOnTalkWithComment(Boolean notifAdminOnTalkWithComment) {
        this.notifAdminOnTalkWithComment = notifAdminOnTalkWithComment;
    }

    @Column(length = 2000)
    public String description;

    @OneToMany(cascade = CascadeType.ALL)
    public List<Lien> liens;

    public List<Lien> getLiens() {
        if (liens == null) {
            liens = new ArrayList<Lien>();
        }
        return liens;
    }

    public String avatar;

    private final static String GRAVATAR_URL = "http://www.gravatar.com/avatar/";

    public String getAvatar() {
        if (avatar == null) {
            String emailHash = DigestUtils.md5Hex(email.toLowerCase().trim());
            avatar = GRAVATAR_URL + emailHash + ".jpg";
        }
        return avatar;
    }

    // -- Queries (long id, user.class)
    public static Model.Finder<Long, User> find = new Model.Finder<Long, User>(Long.class, User.class);

    /**
     * Retrieve a user from an email.
     *
     * @param email email to search
     * @return a user
     */
    public static User findByEmail(String email) {
        return find.where().eq("email", email).findUnique();
    }

    /**
     * Retrieve a user from an Id.
     *
     * @param id id to search
     * @return a user
     */
    public static User findById(Long id) {
        return find.where().eq("id", id).findUnique();
    }

    /**
     * Retrieve a user from an external Id (SocialUser id/providerId).
     *
     * @param uuid uuid to search
     * @return a user
     */
    public static User findByExternalId(String uuid, String providerId) {

    	return find	.where()
    					.eq("extUserId.providerUuid", uuid)
    					.eq("extUserId.providerId", providerId)
    				.findUnique();
    }
    
    /**
     * Retrieve a user from a fullname.
     *
     * @param fullname Full name
     * @return a user
     */
    public static User findByFullname(String fullname) {
        return find.where().eq("fullname", fullname).findUnique();
    }

    /**
     * Retrieves a user from a confirmation token.
     *
     * @param token the confirmation token to use.
     * @return a user if the confirmation token is found, null otherwise.
     */
    public static User findByConfirmationToken(String token) {
        return find.where().eq("confirmationToken", token).findUnique();
    }

    public static List<User> findAll() {
        return find.all();
    }

    public static List<User> findAllAdmin() {
        return find.where().eq("admin", Boolean.TRUE).findList();
    }

}