protected void extractAttachmentsFromMultipart(Multipart mp, Map<String, Attachment> map)
        throws MessagingException, IOException {

        for (int i = 0; i < mp.getCount(); i++) {
            Part part = mp.getBodyPart(i);
            LOG.trace("Part #{}: {}", i, part);

            if (part.isMimeType("multipart/*")) {
                LOG.trace("Part #{}: is mimetype: multipart/*", i);
                extractAttachmentsFromMultipart((Multipart) part.getContent(), map);
            } else {
                String disposition = part.getDisposition();
                String fileName = part.getFileName();

                if (LOG.isTraceEnabled()) {
                    LOG.trace("Part #{}: Disposition: {}", i, disposition);
                    LOG.trace("Part #{}: Description: {}", i, part.getDescription());
                    LOG.trace("Part #{}: ContentType: {}", i, part.getContentType());
                    LOG.trace("Part #{}: FileName: {}", i, fileName);
                    LOG.trace("Part #{}: Size: {}", i, part.getSize());
                    LOG.trace("Part #{}: LineCount: {}", i, part.getLineCount());
                }

                if (validDisposition(disposition, fileName)
                        || fileName != null) {
                    LOG.debug("Mail contains file attachment: {}", fileName);
                    if (!map.containsKey(fileName)) {
                        // Parts marked with a disposition of Part.ATTACHMENT are clearly attachments
                        DefaultAttachment camelAttachment = new DefaultAttachment(part.getDataHandler());
                        @SuppressWarnings("unchecked")
                        Enumeration<Header> headers = part.getAllHeaders();
                        while (headers.hasMoreElements()) {
                            Header header = headers.nextElement();
                            camelAttachment.addHeader(header.getName(), header.getValue());
                        }
                        map.put(fileName, camelAttachment);
                    } else {
                        LOG.warn("Cannot extract duplicate file attachment: {}.", fileName);
                    }
                }
            }
        }
    }