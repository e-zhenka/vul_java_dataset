private void enumerateChmDirectoryListingList(ChmItsfHeader chmItsHeader,
            ChmItspHeader chmItspHeader) throws TikaException {
        try {
            int startPmgl = chmItspHeader.getIndex_head();
            int stopPmgl = chmItspHeader.getUnknown_0024();
            int dir_offset = (int) (chmItsHeader.getDirOffset() + chmItspHeader
                    .getHeader_len());
            setDataOffset(chmItsHeader.getDataOffset());

            /* loops over all pmgls */
            byte[] dir_chunk = null;
            Set<Integer> processed = new HashSet<>();
            for (int i = startPmgl; i>=0; ) {
                dir_chunk = new byte[(int) chmItspHeader.getBlock_len()];
                int start = i * (int) chmItspHeader.getBlock_len() + dir_offset;
                dir_chunk = ChmCommons
                        .copyOfRange(getData(), start,
                                start +(int) chmItspHeader.getBlock_len());

                PMGLheader = new ChmPmglHeader();
                PMGLheader.parse(dir_chunk, PMGLheader);
                enumerateOneSegment(dir_chunk);
                int nextBlock = PMGLheader.getBlockNext();
                processed.add(i);
                if (processed.contains(nextBlock)) {
                    throw new ChmParsingException("already processed block; avoiding cycle");
                }
                i=nextBlock;
                dir_chunk = null;
            }
            System.out.println("done");
        } catch (ChmParsingException e) {
            LOG.warn("Chm parse exception", e);
        } finally {
            setData(null);
        }
    }