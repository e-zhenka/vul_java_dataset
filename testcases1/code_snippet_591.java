@RequestMapping(value="/change_password.do", method = POST)
    public String changePassword(
            Model model,
            @RequestParam("current_password") String currentPassword,
            @RequestParam("new_password") String newPassword,
            @RequestParam("confirm_password") String confirmPassword,
            HttpServletResponse response,
            HttpServletRequest request) {

        PasswordConfirmationValidation validation = new PasswordConfirmationValidation(newPassword, confirmPassword);
        if (!validation.valid()) {
            model.addAttribute("message_code", validation.getMessageCode());
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return "change_password";
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        String username = authentication.getName();

        try {
            changePasswordService.changePassword(username, currentPassword, newPassword);
            request.getSession().invalidate();
            request.getSession(true);
            if (authentication instanceof UaaAuthentication) {
                UaaAuthentication uaaAuthentication = (UaaAuthentication)authentication;
                authentication = new UaaAuthentication(
                    uaaAuthentication.getPrincipal(),
                    new LinkedList<>(uaaAuthentication.getAuthorities()),
                    new UaaAuthenticationDetails(request)
                );
            }
            securityContext.setAuthentication(authentication);
            return "redirect:profile";
        } catch (BadCredentialsException e) {
            model.addAttribute("message_code", "unauthorized");
        } catch (InvalidPasswordException e) {
            model.addAttribute("message", e.getMessagesAsOneString());
        }
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        return "change_password";
    }