/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.search.Search;

/**
 * Created by honchar
 */
public class InducementsTabStorage implements PageStorage{

    private ObjectPaging inducementsPaging;


    @Override
    public Search getSearch() {
        return null;
    }

    @Override
    public void setSearch(Search search) {
    }

    @Override
    public ObjectPaging getPaging() {
        return inducementsPaging;
    }

    @Override
    public void setPaging(ObjectPaging inducementsPaging) {
        this.inducementsPaging = inducementsPaging;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        return "";
    }

}
