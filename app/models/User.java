package models;

import be.objectify.deadbolt.models.Permission;
import be.objectify.deadbolt.models.Role;
import be.objectify.deadbolt.models.RoleHolder;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.ExpressionList;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;
import com.feth.play.module.pa.user.EmailIdentity;
import com.feth.play.module.pa.user.NameIdentity;
import models.utils.AppException;
import models.utils.Hash;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.BooleanUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import service.providers.CfpUsernamePasswordAuthProvider;

import javax.persistence.*;
import java.util.*;

/**
 * User: yesnault
 * Date: 20/01/12
 */
@SuppressWarnings("serial")
@Entity
public class User extends Model implements RoleHolder {

    @Id
    public Long id;

    @Constraints.Required
    @Formats.NonEmpty
    @Column(unique = true)
    public String email;

    @Constraints.Required
    @Formats.NonEmpty
    @Column(unique = true)
    public String fullname;

    public String confirmationToken;

    @Constraints.Required
    @Formats.NonEmpty
    @JsonIgnore
    public String passwordHash;

    @Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date dateCreation;

    @OneToMany(cascade = CascadeType.ALL)
    public List<LinkedAccount> linkedAccounts;

    @JsonProperty(value = "linkSize")
    public int linkSize() {
        return linkedAccounts.size();
    }

    @Formats.NonEmpty
    public Boolean validated = false;

    public Boolean admin = false;

    private Boolean notifOnMyTalk;

    private Boolean notifAdminOnAllTalk;

    private Boolean notifAdminOnTalkWithComment;

    @Constraints.Pattern("^([0-9a-fA-F][0-9a-fA-F]:){5}([0-9a-fA-F][0-9a-fA-F])$")
    public String adresseMac;

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


    private transient String avatar;

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

    public static User findById(Long id) {
        return find.where().eq("id", id).findUnique();
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

    /**
     * Authenticate a User, from a email and clear password.
     *
     * @param email         email
     * @param clearPassword clear password
     * @return User if authenticated, null otherwise
     * @throws AppException App Exception
     */
    public static User authenticate(String email, String clearPassword) throws AppException {

        // get the user with email only to keep the salt password
        User user = find.where().eq("email", email).findUnique();
        if (user != null) {
            // get the hash password from the salt + clear password
            if (Hash.checkPassword(clearPassword, user.passwordHash)) {
                return user;
            }
        }
        return null;
    }

    public void changePassword(String password) throws AppException {
        this.passwordHash = Hash.createPassword(password);
        LinkedAccount linkedAccount =  LinkedAccount.findByProviderKey(this, CfpUsernamePasswordAuthProvider.getProvider().getKey());
        if(linkedAccount != null){
            linkedAccount.providerUserId = this.passwordHash;
            linkedAccount.save();
        }
        this.save();
    }

    public static List<User> findAllAdmin() {
        return find.where().eq("admin", Boolean.TRUE).findList();
    }

    /**
     * Confirms an account.
     *
     * @return true if confirmed, false otherwise.
     * @throws AppException App Exception
     */
    public static boolean confirm(User user) throws AppException {
        if (user == null) {
            return false;
        }

        // If there's no admin for now, the new confirm user is admin.
        if (find.where().eq("admin", Boolean.TRUE).findRowCount() == 0) {
            user.admin = true;
        }
        user.confirmationToken = null;
        user.validated = true;
        user.save();
        return true;
    }


    /**
     * ****
     * <p/>
     * <p/>
     * <p/>
     * *****************
     */

    public static boolean existsByAuthUserIdentity(
            final AuthUserIdentity identity, boolean validated) {
        final ExpressionList<User> exp = getAuthUserFind(identity, validated);
        return exp.findRowCount() > 0;
    }

    public static ExpressionList<User> getAuthUserFind(
            final AuthUserIdentity identity, boolean validated) {
        return find.where().eq("validated", validated)
                .eq("linkedAccounts.providerUserId", identity.getId())
                .eq("linkedAccounts.providerKey", identity.getProvider());
    }

    public static User findByAuthUserIdentity(final AuthUserIdentity identity) {
        if (identity == null) {
            return null;
        }
        return getAuthUserFind(identity, true).findUnique();
    }

    public Set<String> getProviders() {
        final Set<String> providerKeys = new HashSet<String>(
                linkedAccounts.size());
        for (final LinkedAccount acc : linkedAccounts) {
            providerKeys.add(acc.providerKey);
        }
        return providerKeys;
    }

    public static void addLinkedAccount(final AuthUser oldUser,
                                        final AuthUser newUser) {
        final User u = User.findByAuthUserIdentity(oldUser);
        u.linkedAccounts.add(LinkedAccount.create(newUser));
        u.save();
    }

    public LinkedAccount getAccountByProvider(final String providerKey) {
        return LinkedAccount.findByProviderKey(this, providerKey);
    }

    public static User findByUsernamePasswordIdentity(
            final UsernamePasswordAuthUser identity) {
        return getUsernamePasswordAuthUserFind(identity).findUnique();
    }

    private static ExpressionList<User> getUsernamePasswordAuthUserFind(
            final UsernamePasswordAuthUser identity) {
        return getEmailUserFind(identity.getEmail()).eq(
                "linkedAccounts.providerKey", identity.getProvider());
    }

    private static ExpressionList<User> getEmailUserFind(final String email) {
        return find.where().eq("validated", true).eq("email", email);
    }

    public void merge(final User otherUser) {
        for (final LinkedAccount acc : otherUser.linkedAccounts) {
            this.linkedAccounts.add(LinkedAccount.create(acc));
        }
        // do all other merging stuff here - like resources, etc.

        // deactivate the merged user that got added to this one
        otherUser.validated = false;
        Ebean.save(Arrays.asList(new User[]{otherUser, this}));
    }

    public static void merge(final AuthUser oldUser, final AuthUser newUser) {
        User.findByAuthUserIdentity(oldUser).merge(
                User.findByAuthUserIdentity(newUser));
    }

    public static User create(final AuthUser authUser) {
        final User user = new User();
        user.validated = false;

        user.linkedAccounts = Collections.singletonList(LinkedAccount
                .create(authUser));

        if (authUser instanceof EmailIdentity) {
            final EmailIdentity identity = (EmailIdentity) authUser;
            // Remember, even when getting them from FB & Co., emails should be
            // verified within the application as a security breach there might
            // break your security as well!
            user.email = identity.getEmail();
            user.passwordHash = identity.getId();
            user.confirmationToken = UUID.randomUUID().toString();
        }

        if (authUser instanceof NameIdentity) {
            final NameIdentity identity = (NameIdentity) authUser;
            final String name = identity.getName();
            if (name != null) {
                user.fullname = name;
            }
        }
        user.save();
        return user;
    }

    @Override
    public List<? extends Role> getRoles() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<? extends Permission> getPermissions() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
