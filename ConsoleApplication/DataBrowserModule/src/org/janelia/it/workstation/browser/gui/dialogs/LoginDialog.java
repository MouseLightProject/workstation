package org.janelia.it.workstation.browser.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.BiPredicate;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.exceptions.FatalCommError;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.workers.SimpleWorker;

import net.miginfocom.swing.MigLayout;

/**
 * A dialog for entering username and password, with some additional options.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LoginDialog extends ModalDialog {

    public enum ErrorType {
        NetworkError,
        AuthError,
        TokenExpiredError,
        OtherError
    }
    
    private static final String OK_BUTTON_TEXT = "Login";
    
    private final JPanel mainPanel;
    private final JLabel usernameLabel;
    private final JLabel passwordLabel;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JCheckBox rememberCheckbox;
    private final JLabel errorLabel;
    private final JPanel buttonPane; 
    private final JButton cancelButton;
    private final JButton okButton;

    private BiPredicate<String, String> authFunction;
    
    public LoginDialog() {

        setTitle("Login");
        
        mainPanel = new JPanel(new MigLayout("wrap 2"));
        
        usernameLabel = new JLabel("User Name");
        usernameField = new JTextField(20);
        
        passwordLabel = new JLabel("Password");
        passwordField = new JPasswordField(20);
        
        rememberCheckbox = new JCheckBox("Remember Password");
        rememberCheckbox.setSelected(true);

        errorLabel = new JLabel("", UIManager.getIcon("OptionPane.errorIcon"), SwingConstants.LEFT);
        errorLabel.setVisible(false);
        
        mainPanel.add(usernameLabel);
        mainPanel.add(usernameField);
        mainPanel.add(passwordLabel);
        mainPanel.add(passwordField);
        mainPanel.add(rememberCheckbox, "span 2");
        mainPanel.add(errorLabel, "span 2");

        cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel login");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        okButton = new JButton(OK_BUTTON_TEXT);
        okButton.setToolTipText("Attempt to authenticate with the given credentials");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });
                
        getRootPane().setDefaultButton(okButton);
                
        buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.SOUTH);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (StringUtils.isEmpty(usernameField.getText())) {
                    usernameField.requestFocus();
                }
                else if (passwordField.getPassword().length==0) {
                    passwordField.requestFocus();
                }
                else {
                    okButton.requestFocus();
                }
            }
        });
    }

    public void showDialog(BiPredicate<String, String> authFunction) {
        showDialog(authFunction, null);
    }
    
    public void showDialog(BiPredicate<String, String> authFunction, final ErrorType errorType) {
        
        this.authFunction = authFunction;
        
        String username = (String) getModelProperty(AccessManager.USER_NAME, "");
        String password = (String) getModelProperty(AccessManager.USER_PASSWORD, "");
        Boolean remember = (Boolean) getModelProperty(AccessManager.REMEMBER_PASSWORD, Boolean.TRUE);
        
        usernameField.setText(username);
        passwordField.setText(password);
        rememberCheckbox.setSelected(remember);

        if (errorType!=null) {
            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    errorLabel.setText(getErrorMessage(errorType));
                    errorLabel.setVisible(true);
                }
            });
        }
        
        ActivityLogHelper.logUserAction("LoginDialog.showDialog");
        packAndShow();
    }

    private String getErrorMessage(ErrorType errorType) {
        switch (errorType) {
        case NetworkError: return "<html>There was a problem connecting to the server. Please check your network connection and try again.</html>"; 
        case AuthError: return "<html>There is a problem with your username or password. Please try again.</html>"; 
        case TokenExpiredError: return "<html>Your authentication has expired. Please try loggin in again.</html>";
        case OtherError: return "<html>There was a problem loggin in. Please try again.</html>";
        }
        throw new IllegalArgumentException("Unsupported error type: "+errorType);
    }
    
    private void saveAndClose() {

        okButton.setIcon(Icons.getLoadingIcon());
        okButton.setText(null);
    
        errorLabel.setText("");
        errorLabel.setVisible(false);
        
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        FrameworkImplProvider.setModelProperty(AccessManager.USER_NAME, username);
        FrameworkImplProvider.setModelProperty(AccessManager.USER_PASSWORD, rememberCheckbox.isSelected()?password:null);
        FrameworkImplProvider.setModelProperty(AccessManager.REMEMBER_PASSWORD, rememberCheckbox.isSelected());
        
        SimpleWorker worker = new SimpleWorker() {

            private boolean authSuccess;
            
            @Override
            protected void doStuff() throws Exception {
                authSuccess = authFunction.test(username, password);
            }

            @Override
            protected void hadSuccess() {
                okButton.setIcon(null);
                okButton.setText(OK_BUTTON_TEXT);
                if (authSuccess) {
                    setVisible(false);
                }
                else {
                    errorLabel.setText(getErrorMessage(ErrorType.AuthError));
                    errorLabel.setVisible(true);
                }
            }

            @Override
            protected void hadError(Throwable e) {
                FrameworkImplProvider.handleExceptionQuietly(e);
                okButton.setIcon(null);
                okButton.setText(OK_BUTTON_TEXT);
                if (e instanceof FatalCommError) {
                    errorLabel.setText(getErrorMessage(ErrorType.NetworkError));
                    errorLabel.setVisible(true);
                }
                else {
                    errorLabel.setText(getErrorMessage(ErrorType.OtherError));
                    errorLabel.setVisible(true);
                }
            }
        };

        worker.execute();
    }
    
    private Object getModelProperty(String key, Object defaultValue) {
        Object value = FrameworkImplProvider.getModelProperty(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }
}
